package com.zfgc.zfgbb.migrator.converters;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zfgc.zfgbb.migrator.db.PollChoiceDbo;
import com.zfgc.zfgbb.migrator.db.PollChoiceDboExample;
import com.zfgc.zfgbb.migrator.db.PollDbo;
import com.zfgc.zfgbb.migrator.mappers.PollChoiceDboMapper;
import com.zfgc.zfgbb.migrator.smf.dbo.SMFPollChoicesDbExample;
import com.zfgc.zfgbb.migrator.smf.mappers.SMFPollChoicesDbMapper;

@Component
public class PollChoiceConverter {

	@Autowired
	private PollChoiceDboMapper pollChoiceMapper;
	
	@Autowired
	private SMFPollChoicesDbMapper smfPollChoiceMapper;
	
	public Map<Integer,PollChoiceDbo> convertToZfgbb() {
		
		return smfPollChoiceMapper.selectByExample(new SMFPollChoicesDbExample()).stream()
						   .map(smfChoice -> {
							   
							   PollChoiceDbo pollChoice = 
									   PollChoiceDbo.builder()
									   				.activeFlag(true)
									   				.choiceText(smfChoice.getLabel())
									   				.seqno(smfChoice.getIdChoice())
									   				.pollId(smfChoice.getIdPoll())
									   				.votes(smfChoice.getVotes().intValue())
									   				.build();
							   
							   try {
								pollChoice.setMigrationHash(pollChoice.computeHash());
								PollChoiceDboExample pollEx = new PollChoiceDboExample();
								pollEx.createCriteria().andMigrationHashEqualTo(pollChoice.getMigrationHash());
								
								pollChoiceMapper.selectByExample(pollEx).stream().findFirst().ifPresentOrElse(
										poll -> {
											pollChoice.setPollChoiceId(poll.getPollChoiceId());
											pollChoiceMapper.updateByPrimaryKey(pollChoice);
										}, 
										() -> pollChoiceMapper.insert(pollChoice));
								
							} catch (NoSuchAlgorithmException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							   
							   return pollChoice;
						   }).collect(Collectors.toMap(PollChoiceDbo::getPollChoiceId, Function.identity()));
		
		
		
	}
}
