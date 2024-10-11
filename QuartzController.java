@RestController
@RequestMapping("/job")
public class QuartzController {
    @Autowired
    private JobAndTriggerService jobAndTriggerService;


    @Autowired
    private Scheduler scheduler;

    /**
     * 定义规则, 首先用户无法定义到底做什么,用户只能定义什么时候做什么任务,任务是我们预先定义好的
     * 所以我们要求用户传递 什么时间, 做的任务是哪一个
     * @param jobClassName  我们要做的任务的类名
     * @param jobGroupName 我们的任务的组名
     * @param cronExpression 任务的表达式
     */
    @PostMapping("/addjob")
    public void addJob(String jobClassName,String jobGroupName,String cronExpression) throws SchedulerException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        scheduler.start();
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("id","1");
        jobDataMap.put("path","http://test.img");
        JobDetail jobDetail= JobBuilder.newJob(HelloJob.class).withIdentity(jobClassName,jobGroupName).usingJobData(jobDataMap).build();
        CronScheduleBuilder cronScheduleBuilder=CronScheduleBuilder.cronSchedule(cronExpression);
        Trigger trigger=TriggerBuilder.newTrigger().withIdentity(jobClassName,jobGroupName).withSchedule(cronScheduleBuilder).build();
        scheduler.scheduleJob(jobDetail,trigger);

    }

    /**
     * 获取指定字符串的class
     * 因为定时任务中要求的是一个泛型是job类型的class,只有job类型的对象才会有这个class,所以先实例化对象,强转成job,然后再重新获取class
     * @param jobClassName
     * @return
     */
    private Class<? extends Job> getClass(String jobClassName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
       // List list;
       // List<String> list1;
        Class<?> aClass = Class.forName(jobClassName);
        Class<? extends Job> aClass1 = ((Job) aClass.newInstance()).getClass();
        return aClass1;
    }

    @RequestMapping("/queryjob")
    public Map<String, Object> queryJob(int pageNum, int pageSize) {
        PageInfo<JobAndTrigger> pageInfo = jobAndTriggerService.getJobAndTrigger(pageNum, pageSize);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("JobAndTrigger", pageInfo);
        resultMap.put("number", pageInfo.getTotal());
        return resultMap;
    }

    @RequestMapping("/pausejob")
    public void pause(String jobClassName,String jobGroupName) throws SchedulerException {
        scheduler.pauseJob(JobKey.jobKey(jobClassName,jobGroupName));//暂停任务

    }
    @RequestMapping("/resumejob")
    public void resumejob(String jobClassName,String jobGroupName) throws SchedulerException {
        scheduler.resumeJob(JobKey.jobKey(jobClassName,jobGroupName));//恢复任务

    }
    @RequestMapping("/deletejob")
    public void deletejob(String jobClassName,String jobGroupName) throws SchedulerException {
       //先暂停任务
            scheduler.pauseTrigger(TriggerKey.triggerKey(jobClassName,jobGroupName));
        //停止任务
            scheduler.unscheduleJob(TriggerKey.triggerKey(jobClassName,jobGroupName));
        //删除任务
            scheduler.deleteJob(JobKey.jobKey(jobClassName,jobGroupName));
    }


    @RequestMapping("/reschedulejob")
    public void reschedulejob(String jobClassName,String jobGroupName,String cronExpression) throws SchedulerException {
     //更新触发器的时间
        //先找到之前的触发器
        TriggerKey triggerKey=TriggerKey.triggerKey(jobClassName,jobGroupName);
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);//根据表达式获取新的时间规则
        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);//获取原始的触发器
        trigger= trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(cronScheduleBuilder).build();//新的触发器

        scheduler.rescheduleJob(triggerKey, trigger);

    }
}

