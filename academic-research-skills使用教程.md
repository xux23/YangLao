# Academic Research Skills（ARS）简要使用教程

> 基于本机已安装的 v3.20.1 版本（插件目录：`C:\Users\xux\.zcode\cli\plugins\cache\academic-research-skills`），内容取自官方 README、QUICKSTART 与 MODE_REGISTRY。

## 1. 这是什么

Academic Research Skills（ARS）是一套给 Claude Code 用的**学术研究技能包**，覆盖「研究 → 写作 → 评审 → 修订 → 出版」全流程。共 **4 个技能、27 种模式**，每个技能内部由多个分工明确的 Agent 组成（共 30+ 个子 Agent）。官方项目：https://github.com/Imbad0202/academic-research-skills

它的设计理念是**「AI 是副驾驶，不是机长」**：AI 负责搜文献、排格式、验数据、查逻辑一致性等繁琐工作，你负责定义问题、选择方法、解读数据。配套的学术诚信闸门会拦截虚构引用、幻觉实验结论、引用与主张不符等常见 AI 写作失败模式。

## 2. 四个技能的组成

| 技能 | 版本 | Agent 数 | 用途 | 模式数 |
|---|---|---|---|---|
| **deep-research** | v2.12.1 | 13 | 深度研究：文献调研、苏格拉底式引导、系统性回顾 | 8 |
| **academic-paper** | v3.3.1 | 12 | 论文撰写：大纲、初稿、修订、摘要、格式转换 | 11 |
| **academic-paper-reviewer** | v1.11.1 | 7 | 同行评审：多视角审稿、再审验收、方法学检查 | 6 |
| **academic-pipeline** | v3.20.1 | 4+ | 10 阶段全流程调度器：研究→写作→评审→修订→出版 | 1（另含 resume 模式） |

## 3. 安装方式

**方式一：插件一行安装（推荐，v3.7.0 以后）**

```
/plugin marketplace add Imbad0202/academic-research-skills
/plugin install academic-research-skills
```

**方式二：传统 symlink 安装**（用于项目级/全局 skills 目录）：

```bash
git clone https://github.com/Imbad0202/academic-research-skills.git ~/academic-research-skills
# 然后把这四个技能链接到 .claude/skills/ 或 ~/.claude/skills/
```

**可选依赖**：Pandoc（产出 DOCX 用）、tectonic + 思源宋体 TC（编译 APA 7.0 PDF 用）。纯 Markdown 输出不需要它们。

## 4. 快速上手

装好后不需要记命令——**直接用自然语言说要做什么**，ARS 会自动匹配技能和模式：

```
你: "引导我研究 AI 在教育评估中的应用"        → deep-research 的苏格拉底引导
你: "帮我写一篇关于少子化影响的论文"           → academic-paper 完整撰写
你: "帮我审查这篇论文"（接着粘贴论文）         → academic-paper-reviewer 完整审稿
你: "我想做一篇完整的研究论文"                 → academic-pipeline 全流程（10 阶段）
你: "进度" 或 "status"                         → 查看 pipeline 进度
```

常用验证命令：装好后运行 `/ars-plan` 描述你的论文，它会用苏格拉底式对话帮你规划章节结构；或运行 `/ars-lit-review "你的主题"` 做单次文献综述测试。

## 5. 模式速查表

### deep-research（8 种模式）

| 模式 | 输出 | 你说什么会触发 |
|---|---|---|
| full | APA 7.0 完整研究报告（3000–8000 词） | "研究 X 的影响" |
| quick | 快速简报（500–1500 词） | "给我一份 X 的快速摘要" |
| lit-review | 带注释的文献目录 + 综合 | "帮我做文献回顾" |
| three-way-scan | WHY/HOW/WHAT 论文对比短清单 | "对比这几篇论文" |
| fact-check | 逐条说法的验证报告 | "帮我核查这些说法" |
| socratic | 研究计划摘要（多轮引导对话，通常 5–15 轮） | "引导我研究 X" |
| systematic-review | PRISMA 2020 系统性回顾报告 | "做系统性文献回顾，含 PRISMA" |
| review | 对给定文本的评审报告 | "审查这篇论文的研究质量" |

### academic-paper（11 种模式）

| 模式 | 输出 | 你说什么会触发 |
|---|---|---|
| full | 完整论文初稿（IMRaD 或按学科适配） | "帮我写一篇论文" |
| plan | 章节规划（苏格拉底引导） | "引导我写论文" |
| outline-only | 详细大纲 + 证据映射 | "先帮我搭论文大纲" |
| revision | 修订稿 + 逐条回复审稿意见 | "我有初稿和审稿意见" |
| revision-coach | 修订路线图 + 回复信骨架 | "帮我整理审稿意见成修订路线图" |
| abstract-only | 双语摘要（中文 + 英文）+ 关键词 | "帮我写摘要" |
| lit-review | 论文格式的文献回顾 | "把这批数据写成文献回顾论文" |
| format-convert | LaTeX / 指定格式文档 | "转换成 LaTeX" / "引用格式转 IEEE" |
| citation-check | 引用错误报告 | "检查引用格式" |
| disclosure | 目标期刊的 AI 使用声明 | "帮我生成 NeurIPS 的 AI 使用声明" |
| rebuttal-audit | 对现有回复稿的咨询式 QA（不生成内容） | "帮我核查我的审稿回复" |

### academic-paper-reviewer（6 种模式）

| 模式 | 输出 | 你说什么会触发 |
|---|---|---|
| full | 5 份评审报告（期刊匹配审稿人 + 3 位评审 + 魔鬼代言人）+ 编辑决定 + 修订路线图 | "审查这篇论文" |
| quick | 快速评估 | "快速评估这篇论文" |
| guided | 逐问题苏格拉底式对话 | "引导我改进这篇论文" |
| methodology-focus | 深度方法学评审 | "检查研究方法" |
| re-review | 修订验收核查清单 | "验收修订" |
| calibration | 用你的 gold set 测量审查准确率 | "用我的 gold set 校准 reviewer" |

### academic-pipeline（全流程调度器）

| 入口 | 行为 |
|---|---|
| "我想做一篇完整的研究论文" | 从 Stage 1 开始完整 10 阶段流程 |
| "我已经有论文，帮我审查" | 从 Stage 2.5 进入（先做学术诚信审查） |
| "我收到审稿意见了" | 从 Stage 4 进入 |

## 6. 怎么选模式（速查）

| 我想…… | 用这个 |
|---|---|
| 探索一个模糊的想法 | deep-research 的 socratic 模式 |
| 快速要一份文献总结 | deep-research 的 quick 模式 |
| 做系统性综述（PRISMA） | deep-research 的 systematic-review 模式 |
| 从零写一篇论文 | academic-paper 的 full 模式 |
| 一章一章规划论文 | academic-paper 的 plan 模式 |
| 让论文被审一遍 | academic-paper-reviewer 的 full 模式 |
| 全部端到端做掉 | academic-pipeline（说"我要一篇完整的研究论文"） |

## 7. 完整 pipeline 的 10 个阶段

1. 研究（问题定义、文献策略）
2. 写作（大纲 → 初稿）
3. 评审 + 2.5 学术诚信闸门 —— **不可跳过**（拦截虚构引用、幻觉结果、方法伪造等 7 类失败模式）
4. 修订 + 4.5 学术诚信闸门 —— **不可跳过**（同上）
5. 定稿（先问格式：APA 7.0 / Chicago / IEEE）
6. 过程记录（含 6 维度协作质量评估，1–100 分）

特点：每个阶段都有**用户确认的检查点**（FULL/SLIM/MANDATORY 三种）；可以从中间阶段进入（不强制从头跑）；`resume_from_passport` 模式可以跨会话恢复。

**费用与时间预期**：完整跑一篇约 1.5 万字的论文，约 **$4–6 API 费用、2–4 小时协作时间**。

## 8. 输出与格式

- **文件格式**：Markdown（默认）→ DOCX（需 Pandoc）→ LaTeX（APA 7.0 的 `apa7` class / IEEE / Chicago）→ tectonic 编译 PDF
- **引用格式**：APA 7.0 为默认（含中文引用规则）；支持 Chicago、MLA、IEEE、Vancouver，可互相转换
- **论文结构**：IMRaD（实证研究）、主题式文献回顾、理论分析、个案研究、政策简报、研讨会论文
- **语言**：用中文对话默认产出繁体中文，英文对话默认英文；学术论文自动产出**双语摘要**（中文 + 英文）。苏格拉底模式和 Plan 模式用意图匹配识别，**支持任何语言**

## 9. 在 ZCode 环境中使用（本机现状）

本机插件已安装。与 Claude Code 不同，ZCode 中**直接用对话触发整个技能还不完整**——slash 命令（`/ars-plan` 等 16 个）、hooks、自动子 Agent 编排是 Claude Code 专属机制。当前环境直接可用的是 **3 个 Agent 子任务类型**（插件级，位于 `agents/`）：

| Agent 类型 | 职责 |
|---|---|
| `academic-research-skills:research_architect_agent` | 设计研究方法蓝图：选择研究范式、方法、数据策略与分析框架 |
| `academic-research-skills:synthesis_agent` | 跨来源综合：整合研究发现、消解证据冲突、映射知识缺口 |
| `academic-research-skills:report_compiler_agent` | 把研究发现整理成 APA 7.0 学术报告（研究的 Phase 4 / Phase 6 阶段激活） |

**用法建议**：写论文时，可以让我（主对话）按 `academic-paper/SKILL.md` 的协议工作，或按需派发上述 Agent 完成特定环节；完整的 10 阶段 pipeline 编排建议在 Claude Code 中使用本插件。三个 Agent 的完整协议文档在插件目录 `academic-research-skills/academic-paper/agents/`、`deep-research/agents/` 等路径下，可随时按需查阅。

## 10. 常用 slash 命令与环境变量

**Slash 命令**（Claude Code 中可用，共 16 个）：

| 命令 | 作用 |
|---|---|
| `/ars-plan` | 苏格拉底式规划论文章节 |
| `/ars-lit-review` | 文献综述 |
| `/ars-full` | 完整研究/写作流程 |
| `/ars-outline` | 只做大纲 |
| `/ars-revision` | 修订论文 |
| `/ars-revision-coach` | 整理审稿意见成路线图 |
| `/ars-reviewer` | 评审论文 |
| `/ars-abstract` | 写双语摘要 |
| `/ars-3w` | WHY/HOW/WHAT 论文对比 |
| `/ars-rebuttal-audit` | 核查审稿回复稿 |
| `/ars-citation-check` | 检查引用格式 |
| `/ars-format-convert` | 格式/引用转换 |
| `/ars-disclosure` | 生成 AI 使用声明 |
| `/ars-mark-read` / `/ars-unmark-read` | 标记引用已人工核实 |
| `/ars-cache-invalidate` | 清空引用验证缓存 |

**环境变量**（可选开关，默认多数关闭）：

| 变量 | 作用 |
|---|---|
| `ARS_CROSS_MODEL` | 启用第二个 AI 模型独立验证/评审 |
| `ARS_CLAIM_AUDIT=1` | 逐条审计引用是否真的支撑论文主张（发现不符会阻止输出） |
| `ARS_PASSPORT_RESET=1` | 在每个检查点重置上下文（长会话/跨会话恢复用） |
| `ARS_MODEL_TIERING` | 模型分层（`economy` 省成本 / `quality-boost` 提升判断质量） |
| `ARS_UPDATE_CHECK=0` | 关闭更新提醒 |

## 11. 官方资源

- 完整 README（中文）：插件目录下 `README.zh-CN.md`，或 GitHub：https://github.com/Imbad0202/academic-research-skills
- 架构文档（pipeline 流程图、阶段矩阵）：`docs/ARCHITECTURE.md`
- 实际产出展示（中英文 APA 论文全文、学术诚信报告、同行评审报告、回复审稿信）：`examples/showcase/`
- 完整使用指南（繁体中文）：https://open.substack.com/pub/edwardwu223235/p/ai
- 英文全流程演示：https://open.substack.com/pub/edwardwu223235/p/academic-writing-shouldnt-be-a-solo