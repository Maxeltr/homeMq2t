/*
 * The MIT License
 *
 * Copyright 2023 Maxim Eltratov <<Maxim.Eltratov@ya.ru>>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ru.maxeltr.homeMq2t.Mqtt;

/**
 * Ретранслирует pending-сообщения, сохранённые в БД, после успешного
 * восстановления подключения к брокеру.
 *
 * Pending-сообщения появляются, когда клиент отправил PUBLISH (QoS 1/2)
 * или PUBREL (QoS 2), но не получил подтверждение (PUBACK/PUBREC/PUBCOMP)
 * до обрыва соединения. Такие сообщения остаются в БД со статусом
 * {@link ru.maxeltr.homeMq2t.Entity.MqttMessageEntity#STATUS_SEND_PUBLISH}
 * или {@link ru.maxeltr.homeMq2t.Entity.MqttMessageEntity#STATUS_SEND_PUBREL}
 * и должны быть отправлены повторно при переподключении
 * (в пределах той же сессии брокера, cleanSession=false).
 *
 * @author Maxim Eltratov <<Maxim.Eltratov@ya.ru>>
 */
public interface MqttRetransmitService {

    /**
     * Асинхронно читает pending-сообщения из БД и ретранслирует их:
     * <ul>
     * <li>сообщения со статусом SEND_PUBLISH — повторной отправкой PUBLISH
     * (dup=true) с сохранённым packetId;</li>
     * <li>сообщения со статусом SEND_PUBREL — повторной отправкой PUBREL
     * с сохранённым packetId.</li>
     * </ul>
     * Вызывается в двух случаях: периодически по таймеру
     * (свойство retransmit-delay) и после успешного CONNACK
     * ({@link MqttConnectHandler}). Повторные запуски во время
     * идущей ретрансляции пропускаются.
     * При cleanSession=true ретрансляция не выполняется: брокер сбрасывает
     * состояние сессии при обрыве, а зависшие строки в БД очищаются.
     */
    void retransmitPendingMessages();
}
