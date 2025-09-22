package com.xd.xdmanager.frame;

import android.util.Log;

public abstract class PermissionRequest {
    public PermissionRequest() {
    }

    public void onRequest(int code, String[] permissions, int[] grantResults) {
        boolean retn = true;
        if (code == 1) {
            int[] var5 = grantResults;
            int var6 = grantResults.length;

            for (int var7 = 0; var7 < var6; ++var7) {
                int grant = var5[var7];
                if (0 != grant) {
                    retn = false;
                    break;
                }
            }
        }

        if (retn) {
            this.onGrant(permissions, grantResults);
        } else {
            this.onUngrant(permissions, grantResults);
        }

    }

    public abstract void onGrant(String[] var1, int[] var2);

    public void onUngrant(String[] permissions, int[] grantResults) {
        Log.e("PermissionRequest", "--部分权限被拒绝--");
    }
}
