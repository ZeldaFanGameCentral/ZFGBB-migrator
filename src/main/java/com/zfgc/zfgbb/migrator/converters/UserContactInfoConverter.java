package com.zfgc.zfgbb.migrator.converters;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zfgc.zfgbb.migrator.db.EmailAddressDbo;
import com.zfgc.zfgbb.migrator.db.UserContactInfoDbo;
import com.zfgc.zfgbb.migrator.mappers.EmailAddressDboMapper;
import com.zfgc.zfgbb.migrator.mappers.UserContactInfoDboMapper;
import com.zfgc.zfgbb.migrator.smf.dbo.SMFMembersDbExample;
import com.zfgc.zfgbb.migrator.smf.mappers.SMFMembersDbMapper;

@Component
public class UserContactInfoConverter extends AbstractConverter {
	Logger logger = LoggerFactory.getLogger(UserContactInfoConverter.class);
	
	@Autowired
	private SMFMembersDbMapper membersMapper;
	
	@Autowired
	private UserContactInfoDboMapper contactInfoMapper;
	
	@Autowired
	private EmailAddressDboMapper emailMapper;
	
	@Transactional
	public Map<Integer,UserContactInfoDbo> convertToZfgbb() {
		AtomicInteger emailId = new AtomicInteger(1);
		return membersMapper.selectByExample(new SMFMembersDbExample())
					 .stream()
					 .map(member -> {
						 EmailAddressDbo email = new EmailAddressDbo();
						 email.setEmailAddress(member.getEmailAddress());
						 email.setSpammerFlag(member.getIsSpammer() == 1);
						 email.setEmailAddressId(emailId.getAndIncrement());
						 try {
							email.setMigrationHash(email.computeHash());
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						 
						 Optional.ofNullable(emailMapper.selectByPrimaryKey(email.getEmailAddressId()))
						 		 .ifPresentOrElse(existing -> {
						 			 if(!existing.getMigrationHash().equals(email.getMigrationHash())) {
						 				 emailMapper.updateByPrimaryKey(email);
						 			 }
						 		 }, 
						 		 () -> {
						 			 emailMapper.insert(email);
						 		 });
						 
						 
						 UserContactInfoDbo dbo = new UserContactInfoDbo();
						 dbo.setAllowEmailFlag(Boolean.FALSE.equals(member.getHideEmail()));
						 dbo.setAllowPmFlag(true);
						 dbo.setEmailAddressId(email.getEmailAddressId());
						 dbo.setUserId(member.getIdMember());
						 try {
							dbo.setMigrationHash(dbo.computeHash());
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						 
						 Optional.ofNullable(contactInfoMapper.selectByPrimaryKey(dbo.getUserId()))
						 		 .ifPresentOrElse(existing -> {
						 			 if(!existing.getMigrationHash().equals(dbo.getMigrationHash())) {
						 				contactInfoMapper.updateByPrimaryKey(dbo);
						 			 }
						 		 },
						 		 () -> {
						 			 contactInfoMapper.insert(dbo);
						 		 });
						 
						 return dbo;
						 
					 })
					 .collect(Collectors.toMap(UserContactInfoDbo::getUserId, Function.identity()));
	}
}
