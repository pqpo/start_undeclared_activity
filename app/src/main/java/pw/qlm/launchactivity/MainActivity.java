package pw.qlm.launchactivity;

import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Singleton;
import android.view.View;
import android.widget.Button;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btnLaunch;

    @Override
    protected void attachBaseContext(Context newBase) {
        try {
            ActivityManagerNative.getDefault();
            Field gDefault = ActivityManagerNative.class.getDeclaredField("gDefault");
            gDefault.setAccessible(true);
            Object singleton = gDefault.get(null);
            Field mInstance = Singleton.class.getDeclaredField("mInstance");
            mInstance.setAccessible(true);
            final Object oldActivityMgr = mInstance.get(singleton);

            List<Class<?>> interfaces = getAllInterfaces(oldActivityMgr.getClass());
            Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces.toArray(new Class[interfaces.size()]) : new Class[0];

            Object proxyActivityMgr = Proxy.newProxyInstance(newBase.getClassLoader(), ifs, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("startActivity")) {
                        int indexOfIntent = -1;
                        for (int i = 0; i < args.length; i++) {
                            if (args[i] instanceof Intent) {
                                indexOfIntent = i;
                            }
                        }
                        Intent rawIntent = (Intent) args[indexOfIntent];
                        Intent newIntent = new Intent();
                        newIntent.setComponent(new ComponentName("pw.qlm.launchactivity", "pw.qlm.launchactivity.StubActivity"));
                        newIntent.putExtra("rawIntent", rawIntent);
                        args[indexOfIntent] = newIntent;
                    }
                    return method.invoke(oldActivityMgr, args);
                }
            });
            mInstance.set(singleton, proxyActivityMgr);
            Field mH = ActivityThread.class.getDeclaredField("mH");
            mH.setAccessible(true);
            ActivityThread activityThread = ActivityThread.currentActivityThread();
            Object mHandler = mH.get(activityThread);
            Handler.Callback callback = new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    if (msg.what == 100) {
                        Object activityClientRecord = msg.obj;
                        try {
                            Field intent = activityClientRecord.getClass().getDeclaredField("intent");
                            intent.setAccessible(true);
                            Intent intentProxy = (Intent) intent.get(activityClientRecord);
                            Intent rawIntent = (Intent) intentProxy.getExtra("rawIntent");
                            if (rawIntent != null) {
                                intentProxy.setComponent(rawIntent.getComponent());
                                intentProxy.removeExtra("rawIntent");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return false;
                }
            };
            Field mCallback = Handler.class.getDeclaredField("mCallback");
            mCallback.setAccessible(true);
            mCallback.set(mHandler, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnLaunch = (Button) findViewById(R.id.btn_launch);
        btnLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TargetActivity.class));
            }
        });
    }

    public static List<Class<?>> getAllInterfaces(final Class<?> cls) {
        if (cls == null) {
            return null;
        }
        final LinkedHashSet<Class<?>> interfacesFound = new LinkedHashSet<Class<?>>();
        getAllInterfaces(cls, interfacesFound);
        return new ArrayList<Class<?>>(interfacesFound);
    }

    private static void getAllInterfaces(Class<?> cls, final HashSet<Class<?>> interfacesFound) {
        while (cls != null) {
            final Class<?>[] interfaces = cls.getInterfaces();

            for (final Class<?> i : interfaces) {
                if (interfacesFound.add(i)) {
                    getAllInterfaces(i, interfacesFound);
                }
            }

            cls = cls.getSuperclass();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Process.killProcess(Process.myPid());
        System.exit(0);
    }
}
