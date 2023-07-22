package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }

    public String createUser(String name, String mobile) throws Exception {
        if(userMobile.contains(mobile)) throw new Exception("User already exists");
        else {
            userMobile.add(mobile);
            return "SUCCESS";
        }
    }

    public Group createGroup(List<User> users) {
        // The list contains at least 2 users where the first user is the admin. A group has exactly one admin.
        // If there are only 2 users, the group is a personal chat and the group name should be kept as the name of the second user(other than admin)
        // If there are 2+ users, the name of group should be "Group count". For example, the name of first group would be "Group 1", second would be "Group 2" and so on.
        // Note that a personal chat is not considered a group and the count is not updated for personal chats.
        // If group is successfully created, return group.

        //For example: Consider userList1 = {Alex, Bob, Charlie}, userList2 = {Dan, Evan}, userList3 = {Felix, Graham, Hugh}.
        //If createGroup is called for these userLists in the same order, their group names would be "Group 1", "Evan", and "Group 2" respectively.

        Group group;
        if (users.size() == 2) {
            group=new Group(users.get(1).getName(),2);
            groupMessageMap.put(group,new ArrayList<>());
            groupUserMap.put(group, users);
            adminMap.put(group,users.get(0));
            return  group;
        }
        this.customGroupCount+=1;
        group=new Group("Group "+this.customGroupCount, users.size());
        groupUserMap.put(group,users);
        groupMessageMap.put(group,new ArrayList<>());
        adminMap.put(group,users.get(0));
        return group;
    }

    public int createMessage(String content) {
        // The 'i^th' created message has message id 'i'.
        // Return the message id.
        this.messageId+=1;
        Message msg=new Message(messageId,content);
        return msg.getId();

    }

    public int sendMessage(Message message, User sender, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "You are not allowed to send message" if the sender is not a member of the group
        //If the message is sent successfully, return the final number of messages in that group.
        if(!groupUserMap.containsKey(group)){
            throw new Exception("Group does not exist");
        }else {
            boolean isMember=false;
            List<User> userList=groupUserMap.get(group);
            for(User u:userList){
                if(u==sender) {
                    isMember=true;
                    break;
                }
            }
            if(isMember){
                List<Message> msglist=groupMessageMap.get(group);
                msglist.add(message);
                int msgcount=msglist.size();
                groupMessageMap.put(group,msglist);

                senderMap.put(message,sender);
                return msgcount;
            }else {
                throw new Exception("You are not allowed to send message");
            }
        }

    }

    public String changeAdmin(User approver, User user, Group group) throws Exception {
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "Approver does not have rights" if the approver is not the current admin of the group
        //Throw "User is not a participant" if the user is not a part of the group
        //Change the admin of the group to "user" and return "SUCCESS". Note that at one time there is only one admin and the admin rights are transferred from approver to user.

        if(!groupUserMap.containsKey(group)){
            throw new Exception("Group does not exist");
        }else {
            User admin=adminMap.get(group);
            if(admin!=approver){
                throw new Exception("Approver does not have rights");
            }else{
                List<User> userList=groupUserMap.get(group);
                boolean isMember=false;
                for (User u:userList){
                    if(u==user){
                        isMember=true;
                        break;
                    }
                }
                if(!isMember){
                    throw new Exception("User is not a participant");
                }else {
                    adminMap.put(group,user);
                    return "SUCCESS";
                }
            }
        }

    }

    public int removeUser(User user) throws Exception {

        //This is a bonus problem and does not contains any marks
        //A user belongs to exactly one group
        //If user is not found in any group, throw "User not found" exception
        //If user is found in a group and it is the admin, throw "Cannot remove admin" exception
        //If user is not the admin, remove the user from the group, remove all its messages from all the databases, and update relevant attributes accordingly.
        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)
        boolean isUserFound=false;
        Group curGroup=null;
        for(Group g:groupUserMap.keySet()){
            boolean userInList=false;
            List<User> userList=groupUserMap.get(g);
            for(User u:userList){
                if(u==user){
                    userInList=true;
                    curGroup=g;
                    break;
                }
            }
            if(userInList) {
                isUserFound=true;
                break;
            }
        }
        if(!isUserFound){
            throw new Exception("User not found");
        }else{
            User admin=adminMap.get(curGroup);
            if(admin==user){
                throw new Exception("Cannot remove admin");
            }else {
//                remove from usermsgmap
                groupUserMap.get(curGroup).remove(user);
                for (Message msg:senderMap.keySet()){
                    if(senderMap.get(msg)==user){
                        groupMessageMap.get(curGroup).remove(msg);
                    }
                }
//                remove from sendermap
                HashMap<Message, User> updatedSenderMap=new HashMap<>();
                for (Message msg:senderMap.keySet()){
                    if(senderMap.get(msg)==user){
                        continue;
                    }else {
                        updatedSenderMap.put(msg,senderMap.get(msg));
                    }
                }
                senderMap=updatedSenderMap;
                return groupUserMap.get(curGroup).size()+updatedSenderMap.size()+groupMessageMap.get(curGroup).size();
            }
        }



    }

    public String findMessage(Date start, Date end, int k) throws Exception{

        List<Message> messageList = new ArrayList<>();
        for (Group group : groupMessageMap.keySet()) {
            messageList.addAll(groupMessageMap.get(group));
        }

        List<Message> updateMessage = new ArrayList<>();
        for (Message message : messageList) {
            if (message.getTimestamp().after(start) && message.getTimestamp().before(end)) {
                updateMessage.add(message);
            }
        }

        if (updateMessage.size() < k)throw new Exception("K is greater than the number of messages");
        Collections.sort(updateMessage, new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                return o2.getTimestamp().compareTo(o1.getTimestamp());
            }
        });
        return updateMessage.get(k - 1).getContent();
    }
}






