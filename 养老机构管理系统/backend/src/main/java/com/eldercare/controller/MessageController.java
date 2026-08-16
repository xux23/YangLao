package com.eldercare.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.aspect.OperLog;
import com.eldercare.common.Result;
import com.eldercare.dto.MessageDTO;
import com.eldercare.dto.ReplyDTO;
import com.eldercare.entity.Message;
import com.eldercare.security.RequireRole;
import com.eldercare.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 留言反馈接口：家属发表，管理员/护理人员回复
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 家属发表留言
     */
    @OperLog("发表留言")
    @PostMapping
    @RequireRole("family")
    public Result<Void> add(@Valid @RequestBody MessageDTO dto) {
        Message message = new Message();
        message.setElderId(dto.getElderId());
        message.setContent(dto.getContent());
        messageService.addMessage(message);
        return Result.success("留言已提交", null);
    }

    /**
     * 留言分页查询：家属只看关联老人的，机构看全部
     */
    @GetMapping
    public Result<Page<Message>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long elderId) {
        return Result.success(messageService.pageMessage(page, size, status, elderId));
    }

    /**
     * 机构回复留言（管理员、护理人员）
     */
    @OperLog("回复留言")
    @PutMapping("/{id}/reply")
    @RequireRole({"admin", "nurse"})
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody ReplyDTO dto) {
        messageService.replyMessage(id, dto);
        return Result.success("回复成功", null);
    }
}