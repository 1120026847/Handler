package com.example.handler

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.TextView
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.thread
/*
先在子线程中处理耗时操作，当子线程处理完后，通过handler将处理结果传到主线程用来刷新UI，通过handler就完成了线程间的通信

 如果Handler在Activity中是以非静态内部类的方式初始化的，那么Handler默认就会持有Activity的实例，因为在Java中：非静态内部类默认会持有外部类的实例，而静态内部类不会持有外部类的实例

 Handler可能会内存泄露的原因：在Handler中发送延迟消息，如使用sendMessageDelayed(msg, delayMillis)发送消息，并且在msg消息还在MessageQueue中没有得到处理时就关闭了当前页面(Activity调用了finish())，类持有关系是Looper -> MessageQueue -> Message -> Handler -> Activity，而在UI线程中的Looper.loop()是会一直执行的，即UI线程中Looper的生命周期跟Application一样长，从而导致Activity不能及时被回收导致内存泄漏
通过static内部类 + WeakReference弱引用的方式可以避免内存泄漏的产生。
作者：_小马快跑_
链接：https://juejin.cn/post/7129402018107490317
来源：稀土掘金
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。



 */
//todo 关键字转换成非静态内部类 https://juejin.cn/post/7129402018107490317
class MainActivity : AppCompatActivity() {
    private lateinit var tvName: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val num=0
        tvName=findViewById<TextView>(R.id.time);
        val button:Button=findViewById<Button>(R.id.button)
        button.setOnClickListener {
            //新建子线程，在子线程中执行耗时操作，并在子线程中将执行结果通过handler传递到主线程中刷新UI
            Thread {
                //子线程处理完数据，通过handler将结果传到主线程
                //Message.obtain()为新建一个Message，可以从消息池中取消息
                //sendMessageDelayed延迟发送消息，delayMillis为延迟时间
                mHandler.sendMessageDelayed(Message.obtain().apply {
                    what = WHAT
                    obj = "你好"
                }, 500)
            }.start()
        }

    }
    //通过WeakReference弱引用方式解决内存泄漏写
    private val mHandler = MyHandler(WeakReference(this))
    //在主线程中创建Handler。通过handler传递的msg值来刷新UI
// Kotlin中，直接在一个类中声明另一个类，经过Kotlin编译器之后自动就是static静态内部类了,static + 弱引用
    private class MyHandler(val wrActivity: WeakReference<MainActivity>) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            wrActivity.get()?.run {
                ////msg的识别码，用来区别不同的Message
                when (msg.what) {
                    WHAT -> tvName.text = msg.obj as String
                }
            }
        }
    }

    companion object {
        //消息的识别码为100
        const val WHAT = 100
//        message.arg1 = 100; //携带int类型数 100和101
//        message.arg2 = 101;
    }

    override fun onDestroy() {
        //退出页面时，置空所有的Message
        //通过static + 弱引用 + onDestroy中remove Messages避免内存泄漏。
        mHandler.removeCallbacksAndMessages(null)
        super.onDestroy()

    }
/*
 Bundle bundle = new Bundle(); //封装bundle
 bundle.putString("key", "value");
 message.setData(bundle);
 handler.sendMessage(message); //通过handler将Message送到消息队列中
————————————————
版权声明：本文为CSDN博主「-小马快跑-」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/u013700502/article/details/62105858
 */
}