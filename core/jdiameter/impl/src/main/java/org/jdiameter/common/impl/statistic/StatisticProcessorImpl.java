package org.jdiameter.common.impl.statistic;

import static org.jdiameter.common.api.concurrent.IConcurrentFactory.ScheduledExecServices.StatisticTimer;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jdiameter.api.Configuration;
import org.jdiameter.api.StatisticRecord;
import org.jdiameter.common.api.concurrent.IConcurrentFactory;
import org.jdiameter.common.api.statistic.IStatistic;
import org.jdiameter.common.api.statistic.IStatisticManager;
import org.jdiameter.common.api.statistic.IStatisticProcessor;
import org.jdiameter.common.api.statistic.IStatisticRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticProcessorImpl implements IStatisticProcessor {

  private static final Logger log = LoggerFactory.getLogger(StatisticProcessorImpl.class);

  private ScheduledExecutorService executorService;
  private IConcurrentFactory concurrentFactory;
  private IStatisticManager statisticFactory;
  //future for actions to update per second stats
  private Future processorFuture; 
  
  //statics for logger names
  private static final String STATS_ROOT_LOGGER_NAME = "jdiameter.statistic";
  private static final String STATS_LOGGER_PREFIX = "jdiameter.statistic.";
  //future for logger runnable
  private Future logFuture;
  //map of loggers, so we dont have to fetch from slf all the time
  private HashMap<String, Logger> loggers = new HashMap<String, Logger>();
  
  
  public StatisticProcessorImpl(Configuration config, IConcurrentFactory concurrentFactory, final IStatisticManager statisticFactory) {

    this.statisticFactory = statisticFactory;
    this.concurrentFactory = concurrentFactory;

  }

  public void start() {
	  if(!this.statisticFactory.isOn())
	  {
		  return;
	  }
    this.executorService = concurrentFactory.getScheduledExecutorService(StatisticTimer.name());

    //start processor
    this.processorFuture = this.executorService.scheduleAtFixedRate(new Runnable() {
      public void run() {
				try {
					for (IStatisticRecord r : ((StatisticManagerImpl) statisticFactory).getPSStatisticRecord()) {
						StatisticRecord[] recs = r.getChilds();
						//magic of PS records, there are two children created...
						IStatisticRecord realRecord = (IStatisticRecord) recs[0];
						IStatisticRecord prevRecord = (IStatisticRecord) recs[1];
						r.setLongValue(realRecord.getValueAsLong() - prevRecord.getValueAsLong());
						prevRecord.setLongValue(realRecord.getValueAsLong());
					}
				}
        catch (Exception e) {
          log.warn("Can not start persecond statistic", e);
        }
      }
    }, 0, 1, TimeUnit.SECONDS);
    
    
    //start logging actions
    this.executorService.scheduleAtFixedRate(new Runnable() {

        public void run() {
          boolean oneLine = false;
          for (IStatistic statistic : statisticFactory.getStatistic()) {
            if (statistic.isEnabled()) {
              for (StatisticRecord record : statistic.getRecords()) {
                oneLine = true;
                String loggerKey = statistic.getName() + "." + record.getName();
                Logger logger = null;
                if((logger = loggers.get(loggerKey)) == null) {
                  logger = LoggerFactory.getLogger(STATS_LOGGER_PREFIX + loggerKey);
                  loggers.put(loggerKey, logger);
                }
                if(logger.isTraceEnabled()) {
                  logger.trace(record.toString());
                }
              }
            }
          }
          if (oneLine) {
            Logger logger = null;
            if((logger = loggers.get(STATS_ROOT_LOGGER_NAME)) == null) {
              logger = LoggerFactory.getLogger(STATS_ROOT_LOGGER_NAME);
              loggers.put(STATS_ROOT_LOGGER_NAME, logger);
            }
            if(logger.isTraceEnabled()) {
              logger.trace("=============================================== Marker ===============================================");
            }
          }
        }
      }, statisticFactory.getPause(), statisticFactory.getDelay(), TimeUnit.MILLISECONDS);
    
    
  }

  public void stop() {
	  
	  if(!this.statisticFactory.isOn())
	  {
		  return;
	  }
 
    this.processorFuture.cancel(false);
    this.processorFuture = null;
      	
    this.logFuture.cancel(false);
    this.logFuture = null;
    
    
    this.concurrentFactory.shutdownNow(executorService);
  }
}
