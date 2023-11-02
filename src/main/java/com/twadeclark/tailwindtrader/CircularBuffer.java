package com.twadeclark.tailwindtrader;

import java.time.ZonedDateTime;

public class CircularBuffer<S> {
    private final int size;
    private final StockData[] buffer;
    private int head;
    private int tail;

    public CircularBuffer(int size) {
        this.size = size;
        this.buffer = new StockData[size];
        this.head = -1;
        this.tail = 0;
    }

    public void add(ZonedDateTime timestamp, double high, double low, double close) {
        if (get(0) != null && get(0).timestamp.isEqual(timestamp)) {
            // we will get multiple bars for the same timestamp from different exchanges. need to update bar if same timestamp
            // .get(0) is null if it's the very first bar
            get(0).high = Math.max(get(0).high, high);
            get(0).low = Math.min(get(0).low, low);
            get(0).close = close;
        } else { // otherwise just add bar
            head = (head + 1) % size;

            if (buffer[head] == null) {
                buffer[head] = new StockData(timestamp, high, low, close);
            } else { // we re-use the StockData object if there is one already created
                buffer[head].timestamp = timestamp;
                buffer[head].high = high;
                buffer[head].low = low;
                buffer[head].close = close;
            }

            if ((head + 1) % size == tail) {
                tail = (tail + 1) % size;
            }
        }
    }

    public StockData get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        // Calculate the index in reverse order
        int reverseIndex = (head - index + size) % size;

        return buffer[reverseIndex];
    }

    public StockData getHigh(int start, int end) {
        if (start < 0 || start >= size) {
            throw new IndexOutOfBoundsException("start: " + start + ", Size: " + size);
        }
        if (end < 0 || end >= size) {
            throw new IndexOutOfBoundsException("end: " + end + ", Size: " + size);
        }
        if (start > end) {
            throw new IndexOutOfBoundsException("start: " + start + "end: " + end);
        }

        double hiTmp = buffer[start].high;
        int indexTmp = start;

        for(int i=start;i<=end;i++) {
            if (buffer[i].high > hiTmp) {
                hiTmp = buffer[i].high;
                indexTmp = i;
            }
        }

        return buffer[indexTmp];
    }

    public StockData getLow(int start, int end) {
        if (start < 0 || start >= size) {
            throw new IndexOutOfBoundsException("start: " + start + ", Size: " + size);
        }
        if (end < 0 || end >= size) {
            throw new IndexOutOfBoundsException("end: " + end + ", Size: " + size);
        }
        if (start > end) {
            throw new IndexOutOfBoundsException("start: " + start + "end: " + end);
        }

        double loTmp = buffer[start].low;
        int indexTmp = start;

        for(int i=start;i<=end;i++) {
            if (buffer[i].low < loTmp) {
                loTmp = buffer[i].low;
                indexTmp = i;
            }
        }

        return buffer[indexTmp];
    }

    public int Size() {
        return size;
    }
}

