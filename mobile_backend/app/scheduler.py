from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
import logging

logger = logging.getLogger(__name__)

# Create scheduler instance
scheduler = BackgroundScheduler()


def start_scheduler(get_db_func):

    logger.info("start_scheduler: initializing scheduler")

    # =========================
    # DAILY NOTIFICATION JOB
    # =========================

    def daily_notification_job():
        """
        Runs every day at 8:00 PM
        Sends reminders for tomorrow's events/tasks
        """

        from app.services.notification import check_and_notify_all_users

        logger.info("daily_notification_job: triggered")

        db_gen = get_db_func()
        db = next(db_gen)

        try:

            check_and_notify_all_users(db)

        except Exception as e:

            logger.error(f"Notification job failed: {e}", exc_info=True)

        finally:

            try:
                next(db_gen)
            except StopIteration:
                pass

    # =========================
    # REGISTER JOB
    # =========================

    scheduler.add_job(
        daily_notification_job,
        trigger = CronTrigger(hour=0, minute=33),  # 8 AM daily
        id="daily_notification_check",
        replace_existing=True,
    )

    # =========================
    # START SCHEDULER
    # =========================

    if not scheduler.running:
        scheduler.start()
        logger.info("start_scheduler: scheduler started successfully")