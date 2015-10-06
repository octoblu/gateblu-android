package com.octoblu.gateblu.models;

import com.octoblu.sanejsonobject.SaneJSONObject;

/**
 * Created by octoblu on 10/6/15.
 */
public class Device {
    private String connector, type, name, logo, uuid, token;
    private Boolean online, initializing;

    public Device(SaneJSONObject jsonObject) {
        this.fromJSONObject(jsonObject);
    }

    public void fromJSONObject(SaneJSONObject jsonObject){
        this.connector = jsonObject.getStringOrNull("connector");
        this.name = jsonObject.getStringOrNull("name");
        this.logo = jsonObject.getStringOrNull("logo");
        this.uuid = jsonObject.getStringOrNull("uuid");
        this.token = jsonObject.getStringOrNull("token");
        this.type = jsonObject.getStringOrNull("type");
        this.online = jsonObject.getBoolean("online", false);
        this.initializing = jsonObject.getBoolean("initializing", false);
    }

    public String getLogo() {
        if(type == null || type.split(":").length < 2){
            return "https://icons.octoblu.com/device/generic.svg";
        }
        String category = type.split(":")[0];
        String name = type.split(":")[1];

        return "https://icons.octoblu.com/"+category+"/"+name+".svg";
    }

    public String getName(){
        if(this.initializing == true){
            return "Initializing";
        }
        if(this.name == null || this.name.isEmpty()){
            return "Unknown Name";
        }
        return this.name;
    }

    public String getRealName(){
        if(this.name == null || this.name.isEmpty()){
            return this.connector;
        }
        return this.name;
    }

    public String getConnector(){ return this.connector; }
    public String getType(){ return this.type; }
    public String getUuid(){ return this.uuid; }
    public String getToken(){ return this.token; }
    public Boolean getOnline(){ return this.online; }
    public Boolean getInitializing(){ return this.initializing; }
}
