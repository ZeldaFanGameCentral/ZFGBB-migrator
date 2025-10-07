package com.zfgc.zfgbb.migrator.converters;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zfgc.zfgbb.migrator.db.AvatarDbo;
import com.zfgc.zfgbb.migrator.db.AvatarDboExample;
import com.zfgc.zfgbb.migrator.db.ContentResourceDbo;
import com.zfgc.zfgbb.migrator.db.ContentResourceDboExample;
import com.zfgc.zfgbb.migrator.db.GenderLkupDbo;
import com.zfgc.zfgbb.migrator.db.GenderLkupDboExample;
import com.zfgc.zfgbb.migrator.db.UserBioInfoDbo;
import com.zfgc.zfgbb.migrator.db.UserDbo;
import com.zfgc.zfgbb.migrator.mappers.AvatarDboMapper;
import com.zfgc.zfgbb.migrator.mappers.ContentResourceDboMapper;
import com.zfgc.zfgbb.migrator.mappers.GenderLkupDboMapper;
import com.zfgc.zfgbb.migrator.mappers.UserBioInfoDboMapper;
import com.zfgc.zfgbb.migrator.smf.dbo.SMFAttachmentsDb;
import com.zfgc.zfgbb.migrator.smf.dbo.SMFAttachmentsDbExample;
import com.zfgc.zfgbb.migrator.smf.dbo.SMFMembersDbExample;
import com.zfgc.zfgbb.migrator.smf.dbo.SMFMembersDbWithBLOBs;
import com.zfgc.zfgbb.migrator.smf.mappers.SMFAttachmentsDbMapper;
import com.zfgc.zfgbb.migrator.smf.mappers.SMFMembersDbMapper;

@Component
public class UserBioInfoConverter {
	
	@Autowired
	public SMFMembersDbMapper smfMembersMapper;
	
	@Autowired
	public UserBioInfoDboMapper bioInfoMapper;
	
	@Autowired
	private SMFAttachmentsDbMapper smfAttachmentsDbMapper;
	
	@Autowired
	private AvatarDboMapper avatarMapper;
	
	@Autowired
	private ContentResourceDboMapper contentMapper;
	
	@Autowired
	private GenderLkupDboMapper genderLkupMapper;
	
	@Transactional
	public Map<Integer,UserBioInfoDbo> convertToZfgbb() {
		List<SMFMembersDbWithBLOBs> SMFMembers = smfMembersMapper.selectByExampleWithBLOBs(new SMFMembersDbExample());
		Map<Integer,UserBioInfoDbo> result = new HashMap<>();
		
		SMFAttachmentsDbExample avatarEx = new SMFAttachmentsDbExample();
		avatarEx.createCriteria().andIdMemberIsNotNull()
								 .andIdMemberNotEqualTo(0);
		Map<Integer,SMFAttachmentsDb> SMFAvatarAttachments = 
				smfAttachmentsDbMapper.selectByExample(avatarEx).stream()
									  .collect(Collectors.toMap(SMFAttachmentsDb::getIdMember, Function.identity()));
		
		List<GenderLkupDbo> genderLk = genderLkupMapper.selectByExample(new GenderLkupDboExample());
		
		AtomicInteger contentId = new AtomicInteger(1);
		SMFMembers.forEach((smfMember) -> {
			//create the avatar first if one exists for the user
			AvatarDbo avatar = new AvatarDbo();
			SMFAttachmentsDb smfAvatar = SMFAvatarAttachments.get(smfMember.getIdMember());
			if(smfAvatar != null || !smfMember.getAvatar().equals("")) {
				avatar.setUrl(smfMember.getAvatar());
				avatar.setActiveFlag(true);
				
				if(smfAvatar != null) {
					//if the avatar is a user uploaded avatar, create/update content resource record
					ContentResourceDbo contentResource = new ContentResourceDbo();
					contentResource.setContentResourceId(contentId.getAndIncrement());
					contentResource.setContentTypeId(1);
					contentResource.setFilename(smfAvatar.getFilename());
					contentResource.setFileExt(smfAvatar.getFileext());
					contentResource.setMimeType(smfAvatar.getMimeType());
					contentResource.setChecksum(smfAvatar.getFileHash());
					contentResource.setUploadedUserId(smfMember.getIdMember());
					contentResource.setFileSize(smfAvatar.getSize().longValue());
					try {
						contentResource.setMigrationHash(contentResource.computeHash());
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					ContentResourceDboExample resourceEx = new ContentResourceDboExample();
					resourceEx.createCriteria().andContentResourceIdEqualTo(contentResource.getContentResourceId());
					Optional<ContentResourceDbo> existingResource = contentMapper.selectByExample(resourceEx).stream().findFirst();
					
					existingResource.ifPresentOrElse(existing -> {
						if(!existing.getMigrationHash().equals(contentResource.getMigrationHash())) {
							contentMapper.updateByPrimaryKey(contentResource);
						}
					}, 
					() -> {
						contentMapper.insert(contentResource);
					});
					
					avatar.setContentResourceId(contentResource.getContentResourceId());
					avatar.setActiveFlag(smfAvatar.getApproved() == 1);
				}
				
				try {
					avatar.setMigrationHash(avatar.computeHash());
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				AvatarDboExample avEx = new AvatarDboExample();
				avEx.createCriteria().andMigrationHashEqualTo(avatar.getMigrationHash());
				Optional<AvatarDbo> existingAvatar = avatarMapper.selectByExample(avEx).stream().findFirst();
				
				existingAvatar.ifPresentOrElse((existing) -> {
					avatar.setAvatarId(existing.getAvatarId());
					avatarMapper.updateByPrimaryKey(avatar);
				},
				() -> {
					avatarMapper.insert(avatar);
				});
			}
			
			UserBioInfoDbo user = new UserBioInfoDbo();
			
			String genderCode = smfMember.getGender().intValue() == 1 ? "M" : (smfMember.getGender().intValue() == 2 ? "F" : null);
			Integer genderId = genderLk.stream().filter(lk -> lk.getCode().equals(genderCode)).findFirst().map(GenderLkupDbo::getGenderId).orElse(null);
			
			user.setUserId(smfMember.getIdMember());
			user.setCustomTitle(smfMember.getUsertitle());
			user.setPersonalText(smfMember.getPersonalText());
			user.setSignature(smfMember.getSignature());
			user.setAvatarId(avatar.getAvatarId());
			user.setBirthDate(smfMember.getBirthdate());
			user.setKarmaBad(smfMember.getKarmaBad());
			user.setKarmaGood(smfMember.getKarmaGood());
			user.setHideEmailFlag(Boolean.TRUE.equals(smfMember.getHideEmail()));
			user.setHideOnlineStatus(!Boolean.TRUE.equals(smfMember.getShowOnline()));
			user.setGenderId(genderId);
			
			Instant instant = Instant.ofEpochMilli(TimeUnit.SECONDS.toMillis(smfMember.getDateRegistered()));
		    LocalDate dateRegistered =
		      LocalDate.ofInstant(instant, ZoneId.of("UTC"));
			
			user.setDateRegistered(dateRegistered);
			
			
			try {
				user.setMigrationHash(user.computeHash());
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			result.put(user.getUserId(), user);
			
			
			UserBioInfoDbo existingUser = bioInfoMapper.selectByPrimaryKey(user.getUserId());
			if(existingUser == null) {
				bioInfoMapper.insert(user);
			}
			else if(!existingUser.getMigrationHash().equals(user.getMigrationHash())) {
				bioInfoMapper.updateByPrimaryKey(user);
			}
		});
		
		return result;
	}
}
