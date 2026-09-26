package arn;

import java.util.Comparator;

public final class DateComparator implements Comparator<Task> {
    @Override
    public int compare(Task t1, Task t2) {
        return t1.getDate().compareTo(t2.getDate());
    }
}
