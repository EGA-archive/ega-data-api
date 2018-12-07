package eu.elixir.ega.ebi.reencryptionmvc.config;


import eu.elixir.ega.ebi.reencryptionmvc.service.DownloaderLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Profile("log-transfer")
@Component
@Aspect
public class TransferLoggingAspect {

  @Autowired
  private DownloaderLogService downloaderLog;

  @Around("execution(* eu.elixir.ega.ebi.reencryptionmvc.service.ResService.transfer(..))")
  public Object logTransfer(ProceedingJoinPoint pjp) throws Throwable {

    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    Object ret = null;
    Object[] args = pjp.getArgs();

    String requestId = downloaderLog.logStart((String) args[11], (Long) args[7], (Long) args[8]);
    try {
      Object retVal = pjp.proceed();
      long result = (long) retVal;
      ret = retVal;
      stopWatch.stop();
      double speed = (double) result / stopWatch.getTotalTimeMillis();
      downloaderLog.logCompleted(requestId, result, speed);
    } catch (Exception e) {
      downloaderLog.logError(requestId, "transfer", String.format("%s : %s", e.getClass().toString(), e.getMessage()));
      throw e;
    }
    return ret;
  }

}


