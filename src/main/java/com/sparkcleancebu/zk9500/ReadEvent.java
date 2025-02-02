package com.sparkcleancebu.zk9500;

import java.util.EventObject;

@SuppressWarnings("serial")
public class ReadEvent extends EventObject {
	private String base64Template;
	private int retValue;
    	
    public ReadEvent(Object src, String base64, int retValue) {
    	super(src);
    	this.base64Template = base64;
    	this.retValue = retValue;
    }
    	
    public String getBase64Template() {
    	return this.base64Template;
    }
    
    public int getRetValue() {
    	return this.retValue;
    }
}