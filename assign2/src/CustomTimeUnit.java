public enum CustomTimeUnit {
    NANOSECONDS {
        public long toMillis(long duration) {
            return duration / 1_000_000;
        }
    },
    MICROSECONDS {
        public long toMillis(long duration) {
            return duration / 1_000;
        }
    },
    MILLISECONDS {
        public long toMillis(long duration) {
            return duration;
        }
    },
    SECONDS {
        public long toMillis(long duration) {
            return duration * 1_000;
        }
    },
    MINUTES {
        public long toMillis(long duration) {
            return SECONDS.toMillis(duration * 60);
        }
    },
    HOURS {
        public long toMillis(long duration) {
            return MINUTES.toMillis(duration * 60);
        }
    },
    DAYS {
        public long toMillis(long duration) {
            return HOURS.toMillis(duration * 24);
        }
    };

    public abstract long toMillis(long duration);

    public void sleep(long duration) throws InterruptedException {
        Thread.sleep(toMillis(duration));
    }
}
