package io.sufeng.context.domain.service.impl;
import io.sufeng.context.domain.service.ConversationService;
import io.sufeng.context.domain.service.OssObjectServie;
import io.sufeng.context.domain.service.PeopleService;

import java.util.ArrayList;
import java.util.Date;

import io.sufeng.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import io.sufeng.context.domain.ConversationType;
import io.sufeng.context.domain.entity.People;
import io.sufeng.context.domain.entity.message.Conversation;
import io.sufeng.context.domain.entity.message.ConversationUser;
import io.sufeng.context.domain.entity.message.Message;
import io.sufeng.context.domain.entity.message.MessageUser;
import io.sufeng.context.domain.repository.ConversationRepository;
import io.sufeng.context.domain.repository.ConversationUserRepository;
import io.sufeng.context.domain.repository.MessageRepository;
import io.sufeng.context.domain.repository.MessageUserRepository;
import io.sufeng.context.dto.app.ConversationListDto;
import io.sufeng.context.dto.app.EditConversationDto;
import io.sufeng.context.dto.app.MessageDto;
import io.sufeng.context.dto.app.PeopleInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Author zhangchao
 * @Date 2019/5/28 11:21
 * @Version v1.0
 */
@Service
public class ConversationServiceImpl implements ConversationService {


    @Autowired
    ConversationRepository conversationRepository;

    @Autowired
    ConversationUserRepository conversationUserRepository;

    @Autowired
    MessageUserRepository messageUserRepository;

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    PeopleService peopleService;

    @Autowired
    OssObjectServie ossObjectServie;



    @Override
    public String createSingleConversation(String peopleId, String firendsMessageUserId) {
        MessageUser slefMessageUser = messageUserRepository.findById(peopleId).get();
        String slefMessageUserId = slefMessageUser.getId();
        if(StringUtils.equals(slefMessageUserId,firendsMessageUserId)){
            return null;
        }

        String conversationId = conversationRepository.getConversationBySelfAndFirends(slefMessageUserId, firendsMessageUserId);
        if(StringUtils.isNotEmpty(conversationId)){
            return conversationId;
        }

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.SINGLE);
        conversation.setTop(0);
        conversation.setDontDisturb(0);
        conversation.setOwnerPeopleId(slefMessageUserId);
        conversation.setCreateTime(new Date());
        conversation.setLastUpdateTime(new Date());

        conversationRepository.save(conversation);


        ConversationUser conversationUserSlef = new ConversationUser();
        conversationUserSlef.setMessageUserId(slefMessageUserId);
        conversationUserSlef.setConversationId(conversation.getId());
        conversationUserRepository.save(conversationUserSlef);


        ConversationUser conversationUserFirends = new ConversationUser();
        conversationUserFirends.setMessageUserId(firendsMessageUserId);
        conversationUserFirends.setConversationId(conversation.getId());
        conversationUserRepository.save(conversationUserFirends);
        return conversation.getId();
    }

    @Override
    public String createGroupConversation(String peopleId, List<String> firendsMessageUserIds) {
        String conversationId = createSingleConversation(peopleId, firendsMessageUserIds.get(0));

        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        if(conversationOptional.isPresent()){
            Conversation conversation = conversationOptional.get();
            conversation.setType(ConversationType.GROUP);
            conversation.setLastUpdateTime(new Date());

            List<ConversationUser> conversationUserList = conversation.getConversationUserList();

            List<String> userAvatars = new ArrayList<>();

            List<String> collect = conversationUserList.stream().map(item -> item.getMessageUserId())
                    .map(id -> messageUserRepository.findById(id))
                    .map(item -> item.get())
                    .map(item -> item.getId())
                    .map(id -> peopleService.findPeopleById(id).get())
                    .map(p -> {
                        userAvatars.add(p.getEncodedPrincipal());
                        return p.getNickName();
                    })
                    .collect(Collectors.toList());

            String groupName = String.join("@", collect);
            if(groupName.length() > 20){
                groupName = groupName.substring(0,20);
            }

            String icon = ossObjectServie.generateGroupIcon("conversation",userAvatars);
            conversation.setIcon(icon);
            conversation.setName(groupName);
            conversationRepository.saveAndFlush(conversation);
        }

        addMessageUserToGroupConversation(conversationId,firendsMessageUserIds);
        return conversationId;
    }


    @Override
    public String addMessageUserToGroupConversation(String conversationId, List<String> firendsMessageUserIds) {
        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        Conversation conversation = conversationOptional.get();

        for (String firendsMessageUserId : firendsMessageUserIds) {
            if (conversationUserRepository.findByMessageUserIdAndConversationId(firendsMessageUserId,conversationId) == null) {
                ConversationUser conversationUser = new ConversationUser();
                conversationUser.setMessageUserId(firendsMessageUserId);
                conversationUser.setConversationId(conversationId);
                conversationUserRepository.save(conversationUser);
            }
        }

        return conversation.getId();
    }

    @Override
    public List<ConversationListDto> list(String peopleId) {
        MessageUser slefMessageUser = messageUserRepository.findById(peopleId).get();
        String slefMessageUserId = slefMessageUser.getId();
        List<String> conversationIds = conversationUserRepository.findAllByMessageUserId(slefMessageUserId).stream().map(i -> i.getConversationId()).collect(Collectors.toList());

        List<Conversation> conversationList = conversationRepository.findAllById(conversationIds);
        conversationList.stream().forEach(item ->{
            List<ConversationUser> conversationUserList = item.getConversationUserList();
            //拉对方的头像
            Optional<People> optionalPeople = conversationUserList.stream()
                    .filter(i -> !StringUtils.equals(i.getMessageUserId(), slefMessageUserId))
                    .map(i -> i.getMessageUserId())
                    .map(id -> messageUserRepository.findById(id))
                    .map(mu -> peopleService.findPeopleById(mu.get().getId()).get()).findFirst();


            conversationUserList.stream().map(i->i.getMessageUserId())
                    .map(i -> messageUserRepository.findById(i).get().getId())
                    .map(i -> peopleService.findPeopleById(i).get())
                    .forEach(people ->{
                        //todo 要删除的
                        if(people.getEncodedPrincipal().contains("@")){
                            String path = ossObjectServie.generateUserIcon("headimg", people.getNickName());
                            people.setEncodedPrincipal(path);
                            PeopleInfoDto peopleInfoDto = new PeopleInfoDto();
                            peopleInfoDto.setEncodedPrincipal(path);
                            peopleService.updatePeopleInfo(people.getId(),peopleInfoDto);

                            MessageUser messageUser = messageUserRepository.findById(people.getId()).get();
                            messageUser.setEncodedPrincipal(path);
                            messageUserRepository.saveAndFlush(messageUser);
                        }
                    });

            if(optionalPeople.isPresent()){
                People people = optionalPeople.get();
                item.setName(people.getNickName());
                item.setIcon(people.getEncodedPrincipal());
            }
        });


        conversationList.stream().forEach(item->{
            if(item.getType() == ConversationType.SINGLE){
                List<ConversationUser> conversationUserList = item.getConversationUserList();
                //拉对方的头像
                Optional<People> optionalPeople = conversationUserList.stream()
                        .filter(i -> !StringUtils.equals(i.getMessageUserId(), slefMessageUserId))
                        .map(i -> i.getMessageUserId())
                        .map(id -> messageUserRepository.findById(id))
                        .map(mu -> peopleService.findPeopleById(mu.get().getId()).get()).findFirst();

                if(optionalPeople.isPresent()){
                    People people = optionalPeople.get();
                    item.setName(people.getNickName());
                    item.setIcon(people.getEncodedPrincipal());
                }
            }
        });


        List<ConversationListDto> collect = conversationList.stream().map(item -> {
            ConversationListDto conversationListDto = new ConversationListDto();
            conversationListDto.setId(item.getId());
            conversationListDto.setType(item.getType());
            conversationListDto.setName(item.getName());
            conversationListDto.setIcon(item.getIcon());
            conversationListDto.setTags(item.getTags());
            conversationListDto.setTop(item.getTop());
            conversationListDto.setDontDisturb(item.getDontDisturb());
            conversationListDto.setOwnerPeopleId(item.getOwnerPeopleId());
            Message lastMessage = messageRepository.findFirstByConversationIdOrderBySendTimeDesc(item.getId());
            if(lastMessage != null){
                MessageDto messageDto = new MessageDto();
                messageDto.setId(lastMessage.getId());
                messageDto.setMessageType(lastMessage.getMessageType());
                messageDto.setBody(lastMessage.getBody());
                messageDto.setSendTime(lastMessage.getSendTime());
                messageDto.setView(lastMessage.getView());
                conversationListDto.setLastMessage(messageDto);
            }

            return conversationListDto;
        }).collect(Collectors.toList());

        return collect;
    }

    @Override
    public void editConversation(String peopleId, String conversationId, EditConversationDto editConversationDto) {
        MessageUser slefMessageUser = messageUserRepository.findById(peopleId).get();
        String slefMessageUserId = slefMessageUser.getId();

        Optional<Conversation> conversationOptional = conversationRepository.findById(conversationId);
        if(!conversationOptional.isPresent()){
            throw new BusinessException("会话不存在");
        }

        Conversation conversation = conversationOptional.get();
        if(!StringUtils.equals(conversation.getOwnerPeopleId(),slefMessageUserId)){
            throw new BusinessException("只能由群主修改");
        }

        if(StringUtils.isNotEmpty(editConversationDto.getName())){
            conversation.setName(editConversationDto.getName());
        }

        if(StringUtils.isNotEmpty(editConversationDto.getIcon())){
            conversation.setIcon(editConversationDto.getIcon());
        }


        if(editConversationDto.getTags() != null){
            conversation.setTags(String.join("@",editConversationDto.getTags()));
        }

        if(editConversationDto.getTop() != null){
            conversation.setTop(editConversationDto.getTop());
        }

        if(editConversationDto.getDontDisturb() != null){
            conversation.setDontDisturb(editConversationDto.getDontDisturb());
        }
        conversationRepository.saveAndFlush(conversation);
    }


    @Override
    public Conversation getConversation(String conversationId) {
        Optional<Conversation> optionalConversation = conversationRepository.findById(conversationId);
        Conversation item = optionalConversation.get();
        if(item.getType() == ConversationType.GROUP){
            //todo 要删除的
            if(item.getIcon().contains("@")){
                Conversation conversation = item;
                List<String> userAvatars = conversation.getConversationUserList().stream().map(citem -> citem.getMessageUserId())
                        .map(id -> messageUserRepository.findById(id))
                        .map(citem -> citem.get())
                        .map(citem -> citem.getId())
                        .map(id -> peopleService.findPeopleById(id).get())
                        .map(p ->  p.getEncodedPrincipal() )
                        .collect(Collectors.toList());
                String icon = ossObjectServie.generateGroupIcon("conversation",userAvatars);
                item.setIcon(icon);
                conversationRepository.saveAndFlush(item);
            }
        }
        return item;
    }
}
