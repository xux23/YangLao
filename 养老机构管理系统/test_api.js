// 后端接口集成测试：登录、鉴权、五大模块、用药闭环、统计、导出
const BASE = 'http://localhost:8080/api';

let passed = 0;
let failed = 0;

function check(name, ok, extra = '') {
  if (ok) {
    passed++;
    console.log(`  ✅ ${name} ${extra}`);
  } else {
    failed++;
    console.log(`  ❌ ${name} ${extra}`);
  }
}

async function api(method, path, { token, body, raw } = {}) {
  const headers = {};
  if (token) headers['Authorization'] = 'Bearer ' + token;
  if (body) headers['Content-Type'] = 'application/json';
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  });
  if (raw) return res;
  const data = await res.json();
  return { httpStatus: res.status, ...data };
}

async function main() {
  console.log('== 1. 认证 ==');
  // 未登录访问受保护接口 → 401
  const noToken = await api('GET', '/elders');
  check('未登录访问返回 401', noToken.httpStatus === 401 && noToken.code === 401, `(http=${noToken.httpStatus})`);

  // 错误密码
  const badLogin = await api('POST', '/auth/login', { body: { username: 'admin', password: 'wrong' } });
  check('错误密码被拒绝', badLogin.code === 400, `(${badLogin.message})`);

  // admin 登录
  const adminLogin = await api('POST', '/auth/login', { body: { username: 'admin', password: '123456' } });
  check('admin 登录成功', adminLogin.code === 200 && adminLogin.data.token, `(role=${adminLogin.data.user.role})`);
  const adminToken = adminLogin.data.token;

  // nurse01 登录
  const nurseLogin = await api('POST', '/auth/login', { body: { username: 'nurse01', password: '123456' } });
  check('nurse01 登录成功', nurseLogin.code === 200);
  const nurseToken = nurseLogin.data.token;

  // family01 登录
  const familyLogin = await api('POST', '/auth/login', { body: { username: 'family01', password: '123456' } });
  check('family01 登录成功', familyLogin.code === 200);
  const familyToken = familyLogin.data.token;

  console.log('== 2. 权限控制 ==');
  // family 访问用户管理 → 403
  const familyUsers = await api('GET', '/users', { token: familyToken });
  check('家属访问用户管理被拒 403', familyUsers.httpStatus === 403, `(http=${familyUsers.httpStatus})`);
  // nurse 访问统计 → 403
  const nurseStats = await api('GET', '/stats/overview', { token: nurseToken });
  check('护理访问统计看板被拒 403', nurseStats.httpStatus === 403, `(http=${nurseStats.httpStatus})`);
  // family 不能提交护理记录 → 403
  const familyCare = await api('POST', '/care-records', { token: familyToken, body: { elderId: 1, planName: '测试' } });
  check('家属提交护理记录被拒 403', familyCare.httpStatus === 403, `(http=${familyCare.httpStatus})`);

  console.log('== 3. 用户管理 ==');
  const usersPage = await api('GET', '/users?page=1&size=10&role=nurse', { token: adminToken });
  check('用户分页查询(护理)', usersPage.code === 200 && usersPage.data.total === 2, `(total=${usersPage.data.total})`);
  const newUser = await api('POST', '/users', { token: adminToken, body: { username: 'nurse_test', password: '123456', realName: '测试护士', role: 'nurse', phone: '13811112222', status: 1 } });
  check('新增用户', newUser.code === 200, `(${newUser.message})`);
  const dupUser = await api('POST', '/users', { token: adminToken, body: { username: 'nurse_test', password: '123456', realName: '重复', role: 'nurse' } });
  check('重复用户名被拒绝', dupUser.code === 400, `(${dupUser.message})`);
  const resetPwd = await api('PUT', '/users/2/password', { token: adminToken });
  check('重置密码', resetPwd.code === 200);
  // 清理测试用户
  const findTest = await api('GET', '/users?username=nurse_test', { token: adminToken });
  const testId = findTest.data.records[0].id;
  const delTest = await api('DELETE', `/users/${testId}`, { token: adminToken });
  check('删除测试用户', delTest.code === 200);

  console.log('== 4. 老人档案 ==');
  const elderDetail = await api('GET', '/elders/1', { token: adminToken });
  check('老人详情', elderDetail.code === 200 && elderDetail.data.name === '张三', `(${elderDetail.data.name})`);
  const addElder = await api('POST', '/elders', { token: nurseToken, body: { name: '测试老人', gender: 1, birthday: '1950-01-01', idCard: '420000195001010066', phone: '13900000066', emergencyContact: '测试家属', emergencyPhone: '13900000067', healthSummary: '测试数据', familyId: null } });
  check('护理人员新增老人', addElder.code === 200, `(${addElder.message})`);
  const findElder = await api('GET', '/elders?name=测试老人', { token: adminToken });
  const testElderId = findElder.data.records[0].id;
  // 入住登记
  const checkin = await api('POST', `/elders/${testElderId}/checkin`, { token: nurseToken, body: { roomNo: '301', bedNo: 'A', checkinTime: '2026-08-01' } });
  check('入住登记', checkin.code === 200);
  const afterCheckin = await api('GET', `/elders/${testElderId}`, { token: adminToken });
  check('入住后状态/房间', afterCheckin.data.status === 1 && afterCheckin.data.roomNo === '301', `(room=${afterCheckin.data.roomNo})`);
  // 退住登记
  const checkout = await api('POST', `/elders/${testElderId}/checkout`, { token: nurseToken, body: { checkoutTime: '2026-08-16' } });
  check('退住登记', checkout.code === 200);
  const afterCheckout = await api('GET', `/elders/${testElderId}`, { token: adminToken });
  check('退住后释放床位', afterCheckout.data.status === 0 && afterCheckout.data.roomNo === null, `(room=${afterCheckout.data.roomNo})`);
  // 删除测试老人
  const delElder = await api('DELETE', `/elders/${testElderId}`, { token: adminToken });
  check('删除无业务数据老人', delElder.code === 200);

  console.log('== 5. 护理记录 ==');
  const careAdd = await api('POST', '/care-records', { token: nurseToken, body: { elderId: 1, planName: '翻身', planFrequency: '每2小时一次', careContent: '接口测试翻身', remark: '测试备注' } });
  check('新增护理记录', careAdd.code === 200);
  const careList = await api('GET', '/care-records?page=1&size=5&elderId=1', { token: adminToken });
  check('护理记录分页', careList.code === 200 && careList.data.total >= 1 && !!careList.data.records[0].elderName && !!careList.data.records[0].nurseName, `(total=${careList.data.total}, 含姓名补充)`);
  const newCareId = careList.data.records[0].id;
  const careDel = await api('DELETE', `/care-records/${newCareId}`, { token: nurseToken });
  check('删除护理记录(当天)', careDel.code === 200);

  console.log('== 6. 健康体征 ==');
  const healthAdd = await api('POST', '/health-records', { token: nurseToken, body: { elderId: 1, bloodPressure: '120/80', heartRate: 72, temperature: 36.5, bloodSugar: null, remark: '接口测试' } });
  check('新增体征记录', healthAdd.code === 200);
  const healthList = await api('GET', '/health-records?page=1&size=5&elderId=1', { token: adminToken });
  check('体征分页(含老人名)', healthList.code === 200 && !!healthList.data.records[0].elderName);
  const healthId = healthList.data.records[0].id;
  const healthUpd = await api('PUT', `/health-records/${healthId}`, { token: nurseToken, body: { elderId: 1, bloodPressure: '125/85', heartRate: 70, temperature: 36.6, bloodSugar: null } });
  check('修改体征记录', healthUpd.code === 200);
  const healthDel = await api('DELETE', `/health-records/${healthId}`, { token: nurseToken });
  check('删除体征记录', healthDel.code === 200);
  // 健康档案
  const healthDoc = await api('GET', '/elders/1/health', { token: adminToken });
  check('查询健康档案', healthDoc.code === 200 && healthDoc.data.healthSummary.includes('高血压'), `(${healthDoc.data.healthSummary.slice(0, 20)}...)`);
  const healthDocUpd = await api('PUT', '/elders/1/health', { token: nurseToken, body: { healthSummary: '高血压，需低盐饮食，行动不便需轮椅辅助（接口测试更新）' } });
  check('修改健康档案', healthDocUpd.code === 200);

  console.log('== 7. 用药闭环（核心）==');
  // 录入计划（含时间点 07:30 新增一种药）
  const planAdd = await api('POST', '/medicine-plans', { token: nurseToken, body: { elderId: 1, medicineName: '接口测试药片', dosage: '每次1片', times: ['07:30'] } });
  check('录入用药计划', planAdd.code === 200, `(${planAdd.message})`);
  // 查询今日任务（应包含刚生成的 + 原有硝苯地平）
  const today = new Date().toISOString().slice(0, 10);
  const tasks = await api('GET', `/medicine-tasks?date=${today}&elderId=1`, { token: nurseToken });
  check('今日任务(自动生成+逾期扫描)', tasks.code === 200 && tasks.data.length >= 3, `(今日任务数=${tasks.data.length})`);
  // 确认执行其中一个
  const pendingTask = tasks.data.find(t => t.status === 0);
  const complete = await api('PUT', `/medicine-tasks/${pendingTask.id}/complete`, { token: nurseToken });
  check('确认执行任务', complete.code === 200 && complete.data.status === 1 && !!complete.data.confirmTime, `(status=${complete.data.status}, 已记录确认时间)`);
  // 逾期扫描验证：前天未执行的任务应标记为逾期
  const overdue = await api('GET', '/medicine-tasks/overdue?elderId=1', { token: nurseToken });
  check('逾期任务列表(自动扫描)', overdue.code === 200 && overdue.data.length >= 1, `(逾期数=${overdue.data.length})`);
  // 停用接口测试药
  const plans = await api('GET', '/medicine-plans?elderId=1', { token: nurseToken });
  const testPlan = plans.data.find(p => p.medicineName === '接口测试药片');
  const disable = await api('PUT', `/medicine-plans/${testPlan.id}/disable`, { token: nurseToken });
  check('停用用药计划', disable.code === 200);
  const plansAfter = await api('GET', '/medicine-plans?elderId=1', { token: nurseToken });
  check('停用后不再显示该药', !plansAfter.data.some(p => p.medicineName === '接口测试药片'));

  console.log('== 8. 探访预约 ==');
  const visitAdd = await api('POST', '/visits', { token: familyToken, body: { elderId: 1, visitDate: '2026-08-25', visitTime: '上午 9:00-11:00', persons: 2, remark: '接口测试' } });
  check('家属提交预约', visitAdd.code === 200, `(${visitAdd.message})`);
  const familyVisits = await api('GET', '/visits', { token: familyToken });
  check('家属只见自己的预约', familyVisits.code === 200 && familyVisits.data.records.every(v => v.familyName === '张小明'));
  const pendingVisit = familyVisits.data.records.find(v => v.status === 0);
  const auditOk = await api('PUT', `/visits/${pendingVisit.id}/audit`, { token: nurseToken, body: { status: 1, auditRemark: '同意探望' } });
  check('审核通过', auditOk.code === 200);
  const auditReject = await api('PUT', `/visits/${pendingVisit.id}/audit`, { token: nurseToken, body: { status: 2, auditRemark: '' } });
  check('重复审核被拒', auditReject.code === 400, `(${auditReject.message})`);
  const finishVisit = await api('PUT', `/visits/${pendingVisit.id}/finish`, { token: nurseToken });
  check('标记探访完成', finishVisit.code === 200);
  // 驳回流程：重新提交一个预约再走驳回
  await api('POST', '/visits', { token: familyToken, body: { elderId: 1, visitDate: '2026-08-26', visitTime: '下午 14:00-16:00', persons: 1, remark: '' } });
  const pendingVisit2 = (await api('GET', '/visits?status=0', { token: familyToken })).data.records[0];
  const rejectNoRemark = await api('PUT', `/visits/${pendingVisit2.id}/audit`, { token: nurseToken, body: { status: 2, auditRemark: '' } });
  check('驳回必填原因', rejectNoRemark.code === 400, `(${rejectNoRemark.message})`);
  const rejectOk = await api('PUT', `/visits/${pendingVisit2.id}/audit`, { token: nurseToken, body: { status: 2, auditRemark: '人数过多' } });
  check('正常驳回', rejectOk.code === 200);

  console.log('== 9. 留言反馈 ==');
  const msgAdd = await api('POST', '/messages', { token: familyToken, body: { elderId: 1, content: '接口测试留言' } });
  check('家属发表留言', msgAdd.code === 200);
  const msgList = await api('GET', '/messages?page=1&size=5', { token: adminToken });
  const newMsg = msgList.data.records.find(m => m.content === '接口测试留言');
  check('留言列表(机构可见全部)', !!newMsg);
  const msgReply = await api('PUT', `/messages/${newMsg.id}/reply`, { token: nurseToken, body: { reply: '已收到，请放心' } });
  check('机构回复留言', msgReply.code === 200);

  console.log('== 10. 家属数据隔离 ==');
  // family01 访问 elder 2（李秀英，属于 family02）→ 403
  const crossElder = await api('GET', '/elders/2', { token: familyToken });
  check('家属访问他人老人档案 403', crossElder.httpStatus === 403, `(http=${crossElder.httpStatus})`);
  const crossHealth = await api('GET', '/stats/elder/2/health-trend?days=30&metric=heartRate', { token: familyToken });
  check('家属访问他人体征趋势 403', crossHealth.httpStatus === 403, `(http=${crossHealth.httpStatus})`);
  const myElder = await api('GET', '/elders/my', { token: familyToken });
  check('家属获取自己关联老人', myElder.code === 200 && myElder.data.name === '张三', `(${myElder.data.name})`);
  // family 体征列表自动过滤
  const familyHealth = await api('GET', '/health-records', { token: familyToken });
  check('家属体征列表仅含关联老人', familyHealth.code === 200 && familyHealth.data.records.every(r => r.elderId === 1), `(全部 elderId=1)`);

  console.log('== 11. 统计看板 ==');
  const overview = await api('GET', '/stats/overview', { token: adminToken });
  check('看板总览', overview.code === 200 && overview.data.elderTotal >= 5, `(老人${overview.data.elderTotal} 在住${overview.data.inHouse} 入住率${overview.data.checkInRate}% 今日护理${overview.data.todayCareCount})`);
  const ageDist = await api('GET', '/stats/age-distribution', { token: adminToken });
  check('年龄分布', ageDist.code === 200 && ageDist.data.categories.length === 4 && ageDist.data.counts.reduce((a, b) => a + b, 0) >= 5, `(${ageDist.data.categories.join('/')})`);
  const trend = await api('GET', '/stats/activity-trend?days=30', { token: adminToken });
  check('30天趋势(30个日期点)', trend.code === 200 && trend.data.dates.length === 30 && trend.data.careCounts.length === 30, `(护理合计=${trend.data.careCounts.reduce((a, b) => a + b, 0)})`);
  const healthTrend = await api('GET', '/stats/elder/1/health-trend?days=30&metric=bloodPressure', { token: adminToken });
  check('体征趋势(血压拆线)', healthTrend.code === 200 && healthTrend.data.values.some(v => typeof v === 'string' && v.includes('/')), `(${healthTrend.data.values.length} 条)`);

  console.log('== 12. Excel 导出 ==');
  const exportRes = await api('GET', '/elders/export?status=1', { token: nurseToken, raw: true });
  const exportBuf = await exportRes.arrayBuffer();
  check('老人名单导出 xlsx', exportRes.status === 200 && exportRes.headers.get('content-type').includes('spreadsheetml') && exportBuf.byteLength > 500, `(${(exportBuf.byteLength / 1024).toFixed(1)} KB)`);
  const careExport = await api('GET', '/care-records/export?elderId=1', { token: adminToken, raw: true });
  const careBuf = await careExport.arrayBuffer();
  check('护理记录导出 xlsx', careExport.status === 200 && careBuf.byteLength > 500, `(${(careBuf.byteLength / 1024).toFixed(1)} KB)`);

  console.log('== 13. 操作日志 ==');
  const logs = await api('GET', '/logs?page=1&size=10', { token: adminToken });
  // 演示数据 4 条 + 本次测试产生的各类写操作（自动累积），总量应远大于初始值
  check('日志自动记录(总量增长)', logs.code === 200 && logs.data.total > 25, `(total=${logs.data.total})`);
  check('日志含接口与IP', !!logs.data.records[0].method && !!logs.data.records[0].ip, `(${logs.data.records[0].method}, ip=${logs.data.records[0].ip})`);
  const nurseLogs = await api('GET', '/logs', { token: nurseToken });
  check('护理人员查看日志 403', nurseLogs.httpStatus === 403, `(http=${nurseLogs.httpStatus})`);

  console.log(`\n========== 结果: 通过 ${passed} / 失败 ${failed} ==========`);
  process.exit(failed > 0 ? 1 : 0);
}

main().catch(e => { console.error('测试脚本异常:', e.message); process.exit(1); });