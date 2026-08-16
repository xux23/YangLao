package com.eldercare.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.dto.ReplyDTO;
import com.eldercare.entity.Message;

/**
 * 留言反馈业务接口
 */
public interface MessageService {

    /**
     * 家属发表留言（自动绑定当前家属账号）
     */
    void addMessage(Message message);

    /**
     * 留言分页查询：家属只看关联老人的，机构看全部
     */
    Page<Message> pageMessage(int page, int size, Integer status, Long elderId);

    /**
     * 机构回复留言
     */
    void replyMessage(Long id, ReplyDTO dto);
}