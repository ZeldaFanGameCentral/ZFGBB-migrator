package com.zfgc.zfgbb.migrator.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.zfgc.zfgbb.migrator.converters.AttachmentsConverter;
import com.zfgc.zfgbb.migrator.converters.BoardConverter;
import com.zfgc.zfgbb.migrator.converters.CategoryConverter;
import com.zfgc.zfgbb.migrator.converters.IpAddressConverter;
import com.zfgc.zfgbb.migrator.converters.MessageConverter;
import com.zfgc.zfgbb.migrator.converters.MessageHistoryConverter;
import com.zfgc.zfgbb.migrator.converters.ThreadConverter;
import com.zfgc.zfgbb.migrator.converters.UserBioInfoConverter;
import com.zfgc.zfgbb.migrator.converters.UserContactInfoConverter;
import com.zfgc.zfgbb.migrator.converters.UsersConverter;

@Component
public class AppStartUpListener implements ApplicationListener<ContextRefreshedEvent> {
 
	@Autowired
    public UsersConverter userConverter;
	
	@Autowired
	public CategoryConverter catConverter;
	
	@Autowired
	private BoardConverter boardConverter;
	
	@Autowired
	private ThreadConverter threadConverter;
	
	@Autowired
	private MessageConverter messageConverter;
	
	@Autowired
	private IpAddressConverter ipConverter;
     
	@Autowired
	private MessageHistoryConverter msgHistConverter;
	
	@Autowired
	private UserBioInfoConverter bioInfoConverter;
	
	@Autowired
	private AttachmentsConverter attachmentsConverter;
	
	@Autowired
	private UserContactInfoConverter contactInfoConverter;
	
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
         
    	//ORDER MATTERS!!
    	//phase 1 - add some kind of versioning flag instead of commenting this out
		/*
		 * userConverter.convertToZfgbb(); 
		 * catConverter.convertToZfgbb();
		 * boardConverter.convertToZfgbb(); 
		 * threadConverter.convertToZfgbb();
		 * messageConverter.convertToZfgbb(); 
		 * ipConverter.convertToZfgbb();
		 * msgHistConverter.convertToZfgbb();
		 */
        
        //phase 2
    	bioInfoConverter.convertToZfgbb();
    	attachmentsConverter.convertToZfgbb();
    	contactInfoConverter.convertToZfgbb();
    }
}