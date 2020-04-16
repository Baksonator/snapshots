package app.snapshot_bitcake;

import java.io.Serializable;
import java.util.Objects;

public class SnapshotID implements Serializable {

    private static final long serialVersionUID = 6616283323739239536L;

    private final int initId;
    private final int version;

    public SnapshotID(int initId, int version) {
        this.initId = initId;
        this.version = version;
    }

    public int getInitId() {
        return initId;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnapshotID that = (SnapshotID) o;
        return initId == that.initId &&
                version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initId, version);
    }

    @Override
    public String toString() {
        return "SnapshotID{" +
                "initId=" + initId +
                ", version=" + version +
                '}';
    }
}
