package com.mogudiandian.sharding.datasource;

import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * 年范围
 * 如果只有一年 开始和结束相同
 * @author sunbo
 */
@Getter
public class YearRange implements Serializable, Comparable<YearRange> {

    /**
     * 开始年份
     */
    private final int start;

    /**
     * 结束年份（包含）
     */
    private final int end;

    public YearRange(Integer start, Integer end) {
        this.start = Optional.ofNullable(start).orElse(0);
        this.end = Optional.ofNullable(end).orElse(Integer.MAX_VALUE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        YearRange yearRange = (YearRange) o;
        return start == yearRange.start && end == yearRange.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public int compareTo(YearRange o) {
        return this.start - o.start;
    }
}
