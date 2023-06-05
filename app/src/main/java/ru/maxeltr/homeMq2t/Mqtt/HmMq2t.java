
package ru.maxeltr.homeMq2t.Mqtt;

import io.netty.channel.ChannelFuture;

/**
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public interface HmMq2t {

    public ChannelFuture connect();
}
