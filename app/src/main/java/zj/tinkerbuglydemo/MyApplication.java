package zj.tinkerbuglydemo;

import android.app.Application;

import com.tencent.tinker.loader.app.TinkerApplication;
import com.tencent.tinker.loader.shareutil.ShareConstants;

/**
 * Created by Administrator on 2017/11/12.
 */

public class MyApplication extends TinkerApplication {

    public MyApplication() {
        super(ShareConstants.TINKER_ENABLE_ALL, "zj.tinkerbuglydemo.MyApplicationLike",
                "com.tencent.tinker.loader.TinkerLoader", false);
    }

}
