package shx.kitsune;

import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.scheduler.BukkitScheduler;

import com.terheyden.str.Str;

public class StateStore {
    ConcurrentHashMap<String, Object> states = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, SchedulersPairs> schedulers = new ConcurrentHashMap<>();

    public void create(String key, Object value, Boolean force, Boolean sessionBased, Long expire) {
        if (!force) {
            if (states.containsKey(key)) {
                return;
            }
        }

        states.put(key, value);

        if (sessionBased && expire != null && expire > 0) {
            BukkitScheduler scheduler = Kitsune.getPlugin().getServer().getScheduler();

            if (schedulers.containsKey(key)) {
                SchedulersPairs pairs = schedulers.get(key);
                scheduler.cancelTask(pairs.getScheduler());
                schedulers.remove(key);
            }

            schedulers.put(key, new SchedulersPairs(
                scheduler.scheduleSyncDelayedTask(
                    Kitsune.getPlugin(), () -> {
                        remove(key, false);
                    }, expire
                ), expire
            ));
        }
    }

    public Object get(String key) {
        return states.get(key);
    }

    public void remove(String key, Boolean forceCancelSession) {
        if ( schedulers.containsKey(key) && forceCancelSession ) {
            SchedulersPairs pairs = schedulers.get(key);
            BukkitScheduler scheduler = Kitsune.getPlugin().getServer().getScheduler();
            scheduler.cancelTask(pairs.getScheduler());
        }

        states.remove(key);
        schedulers.remove(key);
    }

    public void update(String key, Object value) {
        if ( !states.containsKey(key) ) {
            throw new IllegalStateException(Str.format(
                "Trying to update state that doesn't exist: <{}>", key
            ));
        }

        states.put(key, value);

        if (schedulers.containsKey(key)) {
            BukkitScheduler scheduler = Kitsune.getPlugin().getServer().getScheduler();

            SchedulersPairs pairs = schedulers.get(key);
            scheduler.cancelTask(pairs.getScheduler());

            schedulers.put(key, new SchedulersPairs(
                scheduler.scheduleSyncDelayedTask(
                    Kitsune.getPlugin(), () -> {
                        remove(key, false);
                    }, pairs.getExpires()
                ), pairs.getExpires()
            ));
        }
    }

    public Boolean contains(String key) {
        return states.containsKey(key);
    }
}

class SchedulersPairs {
    private int scheduler;
    private long expires;

    public SchedulersPairs(int scheduler, long expires) {
        this.scheduler = scheduler;
        this.expires = expires;
    }

    public int getScheduler() {
        return scheduler;
    }

    public long getExpires() {
        return expires;
    }

    public void setScheduler(int scheduler) {
        this.scheduler = scheduler;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }
}
