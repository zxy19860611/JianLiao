package com.zoulf.web.jianliao.push.service;

import com.zoulf.web.jianliao.push.bean.api.base.ResponseModel;
import com.zoulf.web.jianliao.push.bean.api.message.MessageCreateModel;
import com.zoulf.web.jianliao.push.bean.card.MessageCard;
import com.zoulf.web.jianliao.push.bean.db.Group;
import com.zoulf.web.jianliao.push.bean.db.Message;
import com.zoulf.web.jianliao.push.bean.db.User;
import com.zoulf.web.jianliao.push.factory.GroupFactory;
import com.zoulf.web.jianliao.push.factory.MessageFactory;
import com.zoulf.web.jianliao.push.factory.PushFactory;
import com.zoulf.web.jianliao.push.factory.UserFactory;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * 消息发送的入口
 *
 * @author qiujuer Email:qiujuer@live.cn
 * @version 1.0.0
 */
@Path("/msg")
public class MessageService extends BaseService {
  // 发送一条消息到服务器
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseModel<MessageCard> pushMessage(MessageCreateModel model) {
    if (!MessageCreateModel.check(model)) {
      return ResponseModel.buildParameterError();
    }

    User self = getSelf();

    // 查询是否已经在数据库中有了
    Message message = MessageFactory.findById(model.getId());
    if (message != null) {
      // 正常返回
      return ResponseModel.buildOk(new MessageCard(message));
    }

    if (model.getReceiverType() == Message.RECEIVER_TYPE_GROUP) {
      return pushToGroup(self, model);
    } else {
      return pushToUser(self, model);
    }
  }

  // 发送到人
  private ResponseModel<MessageCard> pushToUser(User sender, MessageCreateModel model) {
    User receiver = UserFactory.findById(model.getReceiverId());
    if (receiver == null) {
      // 没有找到接收者
      return ResponseModel.buildNotFoundUserError("Con't find receiver user");
    }

    if (receiver.getId().equalsIgnoreCase(sender.getId())) {
      // 发送者接收者是同一个人就返回创建消息失败
      return ResponseModel.buildCreateError(ResponseModel.ERROR_CREATE_MESSAGE);
    }

    // 存储数据库
    Message message = MessageFactory.add(sender, receiver, model);

    return buildAndPushResponse(sender, message);
  }

  // 发送到群
  private ResponseModel<MessageCard> pushToGroup(User sender, MessageCreateModel model) {
    // 找群是有权限性质的找
    Group group = GroupFactory.findById(sender, model.getReceiverId());
    if (group == null) {
      // 没有找到接收者群，有可能是你不是群的成员
      return ResponseModel.buildNotFoundUserError("Con't find receiver group");
    }

    // 添加到数据库
    Message message = MessageFactory.add(sender, group, model);

    // 走通用的推送逻辑
    return buildAndPushResponse(sender, message);
  }

  // 推送并构建一个返回信息
  private ResponseModel<MessageCard> buildAndPushResponse(User sender, Message message) {
    if (message == null) {
      // 存储数据库失败
      return ResponseModel.buildCreateError(ResponseModel.ERROR_CREATE_MESSAGE);
    }

    // 进行推送
    PushFactory.pushNewMessage(sender, message);

    // 返回
    return ResponseModel.buildOk(new MessageCard(message));
  }
}

