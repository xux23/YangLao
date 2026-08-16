package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.BusinessException;
import com.eldercare.dto.ReplyDTO;
import com.eldercare.entity.ElderInfo;
import com.eldercare.entity.Message;
import com.eldercare.entity.SysUser;
import com.eldercare.mapper.ElderInfoMapper;
import com.eldercare.mapper.MessageMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.UserContext;
import com.eldercare.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 家属留言业务实现：家属发表，机构回复，数据隔离
 */
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ElderInfoMapper elderInfoMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Override
    public void addMessage(Message message) {
        ElderInfo elder = elderInfoMapper.selectById(message.getElderId());
        if (elder == null) {
            throw new BusinessException(404, "老人不存在");
        }
        // 家属只能给关联的老人留言
        if (elder.getFamilyId() == null || !elder.getFamilyId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "只能为关联的老人留言");
        }
        Message msg = new Message();
        msg.setElderId(message.getElderId());
        msg.setContent(message.getContent());
        msg.setFamilyId(UserContext.getUserId());
        msg.setStatus(0); // 未回复
        messageMapper.insert(msg);
    }

    @Override
    public Page<Message> pageMessage(int page, int size, Integer status, Long elderId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        if ("family".equals(UserContext.getRole())) {
            // 家属只看关联老人的留言
            ElderInfo elder = elderInfoMapper.selectOne(
                    new LambdaQueryWrapper<ElderInfo>().eq(ElderInfo::getFamilyId, UserContext.getUserId()));
            if (elder != null) {
                wrapper.eq(Message::getElderId, elder.getId());
            } else {
                // 没有关联老人时返回空结果
                return new Page<>(page, size);
            }
        } else {
            wrapper.eq(status != null, Message::getStatus, status)
                    .eq(elderId != null, Message::getElderId, elderId);
        }
        wrapper.orderByDesc(Message::getId);
        Page<Message> result = messageMapper.selectPage(new Page<>(page, size), wrapper);
        fillNames(result.getRecords());
        return result;
    }

    @Override
    public void replyMessage(Long id, ReplyDTO dto) {
        Message message = messageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException(404, "留言不存在");
        }
        if (message.getStatus() != null && message.getStatus() == 1) {
            throw new BusinessException(400, "该留言已回复，请勿重复操作");
        }
        message.setReply(dto.getReply());
        message.setReplyTime(LocalDateTime.now());
        message.setStatus(1);
        messageMapper.updateById(message);
    }

    /**
     * 补充老人姓名与家属姓名
     */
    private void fillNames(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        Set<Long> elderIds = new HashSet<>();
        Set<Long> familyIds = new HashSet<>();
        messages.forEach(m -> {
            elderIds.add(m.getElderId());
            familyIds.add(m.getFamilyId());
        });
        Map<Long, ElderInfo> elderMap = elderInfoMapper.selectBatchIds(elderIds).stream()
                .collect(Collectors.toMap(ElderInfo::getId, Function.identity()));
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(familyIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        messages.forEach(m -> {
            if (elderMap.containsKey(m.getElderId())) {
                m.setElderName(elderMap.get(m.getElderId()).getName());
            }
            if (userMap.containsKey(m.getFamilyId())) {
                m.setFamilyName(userMap.get(m.getFamilyId()).getRealName());
            }
        });
    }
}