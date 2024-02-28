package com.godaddy.hfs.sysadmin;

public class SysAdminAction {
    public long sysAdminActionId;
    public long cncRequestId;
    public long vmId;
    public String createdAt;
    public String modifiedAt;
    public String completedAt;
    public Status status;
    public Type type;
    public String message;
    public String resultSet;
    public String workflowId;
    private static final Status[] statusById = new Status[Status.values().length + 1];
    private static final Type[] typeById;

    public SysAdminAction() {
    }

    public String toString() {
        return "SysAdminAction [sysAdminActionId=" + this.sysAdminActionId + ", cncRequestId=" + this.cncRequestId + ", status=" + this.status + ", vmId=" + this.vmId + ", createdAt=" + this.createdAt + ", modifiedAt=" + this.modifiedAt + ", completedAt=" + this.completedAt + ", message=" + this.message + ", resultSet=" + this.resultSet + ", workflowId=" + this.workflowId + "]";
    }

    public static Status getStatusByid(int statusId) {
        return statusById[statusId];
    }

    public static Type getTypeByid(int typeId) {
        return typeById[typeId];
    }

    static {
        Status[] var0 = Status.values();
        int var1 = var0.length;

        int var2;
        for(var2 = 0; var2 < var1; ++var2) {
            Status status = var0[var2];
            statusById[status.id] = status;
        }

        typeById = new Type[Type.values().length + 1];
        Type[] var4 = Type.values();
        var1 = var4.length;

        for(var2 = 0; var2 < var1; ++var2) {
            Type type = var4[var2];
            typeById[type.id] = type;
        }

    }

    public static enum Type {
        ADD_USER(1),
        REMOVE_USER(2),
        CHANGE_PASSWORD(3),
        CHANGE_HOSTNAME(4),
        INSTALL(5),
        UNINSTALL(6),
        USAGE_STATS(7),
        CONFIGURE_MTA(8),
        ENABLE_ADMIN(9),
        DISABLE_ADMIN(10),
        ADD_FTP_USER(11),
        REMOVE_FTP_USER(12),
        CHANGE_FTP_PASSWORD(13),
        ADD_DB_USER(14),
        REMOVE_DB_USER(15),
        CHANGE_DB_PASSWORD(16),
        ADD_DB(17),
        REMOVE_DB(18),
        ADD_SITE(19),
        REMOVE_SITE(20),
        INSTALL_NYDUS(21),
        REMOVE_NYDUS(22),
        INSTALL_PANOPTA(23),
        GET_PANOPTA_SERVER_KEY(24),
        DELETE_PANOPTA(25),
        ENABLE_WINEXE(26),
        WRITE_FILE(27),
        RESTART_SERVICE(28),
        OPEN_PORT(29),
        CLOSE_PORT(30),
        GET_FILE_INFO(31),
        INSTALL_QEMU_AGENT(32),
        UPDATE_NYDUS(33),
        ADD_SUPPORT_USER(34);

        public final int id;

        private Type(int id) {
            this.id = id;
        }
    }

    public static enum Status {
        NEW(1),
        IN_PROGRESS(2),
        COMPLETE(3),
        FAILED(4);

        public final int id;

        private Status(int id) {
            this.id = id;
        }
    }
}
