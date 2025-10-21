package com.zfgc.zfgbb.migrator.converters;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zfgc.zfgbb.migrator.db.PollChoiceDbo;
import com.zfgc.zfgbb.migrator.db.PollChoiceDboExample;
import com.zfgc.zfgbb.migrator.db.UserPollChoiceDbo;
import com.zfgc.zfgbb.migrator.db.UserPollChoiceDboExample;
import com.zfgc.zfgbb.migrator.mappers.PollChoiceDboMapper;
import com.zfgc.zfgbb.migrator.mappers.UserPollChoiceDboMapper;
import com.zfgc.zfgbb.migrator.smf.dbo.SMFLogPollsDbExample;
import com.zfgc.zfgbb.migrator.smf.mappers.SMFLogPollsDbMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserPollChoiceConverter {

	@Autowired
	private UserPollChoiceDboMapper userPollChoiceMapper;
	
	@Autowired
	private PollChoiceDboMapper pollQuestionMapper;
	
	@Autowired
	private SMFLogPollsDbMapper smfLogPollsMapper;
	
	public Map<Integer, UserPollChoiceDbo> convertToZfgbb(){
		Map<String, Integer> pollChoiceMap = pollQuestionMapper.selectByExample(new PollChoiceDboExample())
																 .stream()
																 .collect(Collectors.toMap(q -> q.getPollId() + "," + q.getSeqno(), PollChoiceDbo::getPollChoiceId));
		
		SMFLogPollsDbExample logEx = new SMFLogPollsDbExample();
		logEx.createCriteria().andIdMemberNotEqualTo(0);
		return smfLogPollsMapper.selectByExample(logEx)
						 .stream()
						 .map(l -> {
							 Integer choiceId = pollChoiceMap.get(l.getIdPoll() + "," + l.getIdChoice());
							 
							 UserPollChoiceDbo pollChoiceDbo = UserPollChoiceDbo.builder()
							 				  .pollChoiceId(choiceId)
							 				  .userId(l.getIdMember())
							 				  .build();
							 
							 
							 try {
								pollChoiceDbo.setMigrationHash(pollChoiceDbo.computeHash());
								UserPollChoiceDboExample ex = new UserPollChoiceDboExample();
								ex.createCriteria().andMigrationHashEqualTo(pollChoiceDbo.getMigrationHash());
								userPollChoiceMapper.selectByExample(ex).stream().findFirst()
													.ifPresentOrElse(
															upc -> {
																pollChoiceDbo.setUserPollChoiceId(upc.getUserPollChoiceId());
																userPollChoiceMapper.updateByPrimaryKey(pollChoiceDbo);
															}, 
															() -> userPollChoiceMapper.insert(pollChoiceDbo)
													);
							} catch (NoSuchAlgorithmException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							 
							 //log.info(pollChoiceDbo.getMigrationHash());
							 
							 return pollChoiceDbo;			  
							 
							 
						 }).collect(Collectors.toMap(UserPollChoiceDbo::getUserPollChoiceId, Function.identity()));
		
		
	}
	
}
