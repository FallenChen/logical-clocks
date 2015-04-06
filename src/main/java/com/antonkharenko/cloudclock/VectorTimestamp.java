package com.antonkharenko.cloudclock;

import java.util.Arrays;

/**
 * @author Anton Kharenko
 */
public final class VectorTimestamp {

	// TODO: consider string/object identifier instead of numeric index and Map/Set/List data structure instead of array (?)

	private final LogicalTimestamp[] timestamps;

	public VectorTimestamp(int vectorLength) {
		timestamps = new LogicalTimestamp[vectorLength];
		for (int i = 0; i < vectorLength; i++) {
			timestamps[i] = new LogicalTimestamp();
		}
	}

	public VectorTimestamp(LogicalTimestamp[] timestamps) {
		this.timestamps = Arrays.copyOf(timestamps, timestamps.length);
	}

	public VectorTimestamp nextTimestamp(int localIndex) {
		if (localIndex < 0 || localIndex >= timestamps.length)
			throw new IllegalArgumentException("Index out of bounds");

		LogicalTimestamp[] newTimestamps = Arrays.copyOf(timestamps, timestamps.length);
		newTimestamps[localIndex] = newTimestamps[localIndex].nextTimestamp();

		return new VectorTimestamp(newTimestamps);
	}

	public VectorTimestamp nextTimestamp(int localIndex, VectorTimestamp happensBeforeTimestamp) {
		if (localIndex < 0 || localIndex >= timestamps.length)
			throw new IllegalArgumentException("Index out of bounds.");
		if (timestamps.length != happensBeforeTimestamp.timestamps.length)
			throw new IllegalArgumentException("Timestamp vectors length do not match.");

		LogicalTimestamp[] newTimestamps = Arrays.copyOf(timestamps, timestamps.length);
		newTimestamps[localIndex] = newTimestamps[localIndex].nextTimestamp();
		for (int i = 0; i < newTimestamps.length; i++) {
			if (i != localIndex && newTimestamps[i].isBefore(happensBeforeTimestamp.timestamps[i])) {
				newTimestamps[i] = happensBeforeTimestamp.timestamps[i];
			}
		}

		return new VectorTimestamp(newTimestamps);
	}

	/**
	 * Returns true if current timestamp happens before given timestamp and timestamps are in a causal relation.
	 * In case of false result it either can correspond to the one of possible situations: timestamps are equal,
	 * timestamps are concurrent or this timestamp happens after the given timestamp.
	 */
	public boolean isHappensBefore(VectorTimestamp that) {
		return getRelation(that) == Relation.HAPPENS_BEFORE;
	}

	/**
	 * Returns true if current timestamp happens after given timestamp and timestamps are in a causal relation.
	 * In case of false result it either can correspond to the one of possible situations: timestamps are equal,
	 * timestamps are concurrent or this timestamp happens before the given timestamp.
	 */
	public boolean isHappensAfter(VectorTimestamp that) {
		return getRelation(that) == Relation.HAPPENS_AFTER;
	}

	/**
	 * Returns true if current timestamp happens concurrently and there is no causal relation between them.
	 * In case of false result it either can correspond to the one of possible situations: timestamps are equal,
	 * timestamps are in causal relation.
	 */
	public boolean isConcurrent(VectorTimestamp that) {
		return getRelation(that) == Relation.HAPPENS_BEFORE;
	}

	/**
	 * Defines relation between this timestamp and given timestamp. Two timestamps may be equal, concurrent or
	 * in causal (happens-before) relation.
	 *
	 * @param that given timestamp to compare
	 * @return {@code Relation} between current timestamp and the given one.
	 *
	 * @see com.antonkharenko.cloudclock.Relation
	 */
	public Relation getRelation(VectorTimestamp that) {
		if (timestamps.length != that.timestamps.length)
			throw new IllegalArgumentException("Timestamp vectors length do not match.");

		Relation relation = Relation.EQUAL;
		for (int i = 0; i < timestamps.length; i++) {
			if (this.timestamps[i].isBefore(that.timestamps[i])) {
				if (relation == Relation.HAPPENS_AFTER)
					return Relation.CONCURRENT;
				relation = Relation.HAPPENS_BEFORE;
			} else if (this.timestamps[i].isAfter(that.timestamps[i])) {
				if (relation == Relation.HAPPENS_BEFORE)
					return Relation.CONCURRENT;
				relation = Relation.HAPPENS_AFTER;
			}
		}

		return relation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		VectorTimestamp that = (VectorTimestamp) o;
		return Arrays.equals(timestamps, that.timestamps);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(timestamps);
	}

	@Override
	public String toString() {
		return "VectorTimestamp{" +
				"timestamps=" + Arrays.toString(timestamps) +
				'}';
	}
}
