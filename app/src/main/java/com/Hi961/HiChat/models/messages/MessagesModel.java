package com.Hi961.HiChat.models.messages;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Abderrahim El imame on 20/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class MessagesModel extends RealmObject {
    @PrimaryKey
    private int id;
    private String message;
    private String date;
    private String username;
    private String phone;
    private int status;
    private boolean isGroup;
    private int conversationID;
    private int senderID;
    private int groupID;
    private int recipientID;

    private String imageFile;
    private String videoThumbnailFile;
    private String videoFile;
    private String audioFile;
    private String documentFile;
    private boolean isFileUpload;
    private boolean isFileDownLoad;
    private String FileSize;
    private String Duration;

    public MessagesModel() {

    }


    public String getDuration() {
        return Duration;
    }

    public void setDuration(String duration) {
        Duration = duration;
    }

    public String getVideoThumbnailFile() {
        return videoThumbnailFile;
    }

    public void setVideoThumbnailFile(String videoThumbnailFile) {
        this.videoThumbnailFile = videoThumbnailFile;
    }


    public String getFileSize() {
        return FileSize;
    }

    public void setFileSize(String fileSize) {
        FileSize = fileSize;
    }

    public boolean isFileDownLoad() {
        return isFileDownLoad;
    }

    public void setFileDownLoad(boolean fileDownLoad) {
        isFileDownLoad = fileDownLoad;
    }

    public String getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(String audioFile) {
        this.audioFile = audioFile;
    }

    public String getDocumentFile() {
        return documentFile;
    }

    public void setDocumentFile(String documentFile) {
        this.documentFile = documentFile;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getGroupID() {
        return groupID;
    }

    public void setGroupID(int groupID) {
        this.groupID = groupID;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }


    public int getSenderID() {
        return senderID;
    }

    public void setSenderID(int senderID) {
        this.senderID = senderID;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getConversationID() {
        return conversationID;
    }

    public void setConversationID(int conversationID) {
        this.conversationID = conversationID;
    }

    public int getRecipientID() {
        return recipientID;
    }

    public void setRecipientID(int recipientID) {
        this.recipientID = recipientID;
    }

    public String getImageFile() {
        return imageFile;
    }

    public void setImageFile(String imageFile) {
        this.imageFile = imageFile;
    }

    public String getVideoFile() {
        return videoFile;
    }

    public void setVideoFile(String videoFile) {
        this.videoFile = videoFile;
    }

    public boolean isFileUpload() {
        return isFileUpload;
    }

    public void setFileUpload(boolean fileUpload) {
        isFileUpload = fileUpload;
    }
}
