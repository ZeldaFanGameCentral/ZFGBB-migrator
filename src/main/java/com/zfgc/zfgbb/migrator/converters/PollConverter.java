package com.zfgc.zfgbb.migrator.converters;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zfgc.zfgbb.migrator.db.PollDbo;
import com.zfgc.zfgbb.migrator.db.PollDboExample;
import com.zfgc.zfgbb.migrator.db.ThreadDboExample;
import com.zfgc.zfgbb.migrator.mappers.PollDboMapper;
import com.zfgc.zfgbb.migrator.mappers.ThreadDboMapper;
import com.zfgc.zfgbb.migrator.smf.dbo.SMFPollsDb;
import com.zfgc.zfgbb.migrator.smf.dbo.SMFPollsDbExample;
import com.zfgc.zfgbb.migrator.smf.dbo.SMFTopicDb;
import com.zfgc.zfgbb.migrator.smf.dbo.SMFTopicDbExample;
import com.zfgc.zfgbb.migrator.smf.mappers.SMFPollsDbMapper;
import com.zfgc.zfgbb.migrator.smf.mappers.SMFTopicDbMapper;

@Component
public class PollConverter extends AbstractConverter {

	@Autowired
	private SMFPollsDbMapper smfPollsMapper;
	
	@Autowired
	private PollDboMapper pollMapper;
	
	@Autowired
	private SMFTopicDbMapper threadMapper;
	
	public Map<Integer,PollDbo> convertToZfgbb() {
		List<SMFPollsDb> smfPolls = smfPollsMapper.selectByExample(new SMFPollsDbExample());
		SMFTopicDbExample threadEx = new SMFTopicDbExample();
		threadEx.createCriteria().andIdPollIsNotNull().andIdPollNotEqualTo(0);
		Map<Integer, Integer> threadMap = threadMapper.selectByExample(threadEx).stream().collect(Collectors.toMap(SMFTopicDb::getIdPoll, SMFTopicDb::getIdTopic));
		
		return smfPolls.stream()
				.map(smfPoll -> {
					PollDbo poll = new PollDbo();
					
					Instant instant = Instant.ofEpochMilli(TimeUnit.SECONDS.toMillis(smfPoll.getExpireTime()));
				    LocalDateTime expireTime =
				      LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));

					poll.setChangeVoteFlag(smfPoll.getChangeVote());
					poll.setCreatedTs(LocalDateTime.now());
					poll.setCreatedUserId(smfPoll.getIdMember());
					poll.setExpireTime(smfPoll.getExpireTime() == 0 ? null : expireTime);
					poll.setGuestVoteCount(smfPoll.getNumGuestVoters());
					poll.setGuestVoteFlag(smfPoll.getGuestVote());
					poll.setHideResultsFlag(smfPoll.getHideResults());
					poll.setMaxVotes(smfPoll.getMaxVotes());
					poll.setPollId(smfPoll.getIdPoll());
					poll.setPollQuestion(smfPoll.getQuestion());
					poll.setResetPoll(smfPoll.getResetPoll());
					poll.setThreadId(smfPoll.getIdTopic());
					poll.setVotingLockedFlag(smfPoll.getVotingLocked());
					poll.setPollQuestion(smfPoll.getQuestion());
					poll.setThreadId(threadMap.get(poll.getPollId()));
					
					
					try {
						poll.setMigrationHash(poll.computeHash());
						PollDboExample pollEx = new PollDboExample();
						pollEx.createCriteria().andMigrationHashEqualTo(poll.getMigrationHash());
						pollMapper.selectByExample(pollEx).stream().findFirst().ifPresentOrElse(
								pollDb -> pollMapper.updateByPrimaryKey(poll), 
								() -> pollMapper.insert(poll));
						
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					return poll;
				})
				.collect(Collectors.toMap(PollDbo::getPollId, Function.identity()));
	}
	
}
