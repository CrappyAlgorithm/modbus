/*
 * Copyright 2016 Kevin Herron
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.digitalpetri.modbus.examples.slave;

import java.util.concurrent.ExecutionException;

import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.slave.ModbusTcpSlave;
import com.digitalpetri.modbus.slave.ModbusTcpSlaveConfig;
import com.digitalpetri.modbus.slave.ServiceRequestHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlaveExample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final SlaveExample slaveExample = new SlaveExample();

        slaveExample.start();

        Runtime.getRuntime().addShutdownHook(new Thread("modbus-slave-shutdown-hook") {
            @Override
            public void run() {
                slaveExample.stop();
            }
        });

        Thread.sleep(Integer.MAX_VALUE);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ModbusTcpSlaveConfig config = new ModbusTcpSlaveConfig.Builder().build();
    private final ModbusTcpSlave slave = new ModbusTcpSlave(config);

    public SlaveExample() {}

    public void start() throws ExecutionException, InterruptedException {
        slave.setRequestHandler(new ServiceRequestHandler() {
            @Override
            public void onReadHoldingRegisters(ServiceRequest<ReadHoldingRegistersRequest, ReadHoldingRegistersResponse> service) {
                String clientRemoteAddress = service.getChannel().remoteAddress().toString();
                String clientIp = clientRemoteAddress.replaceAll(".*/(.*):.*", "$1");
                String clientPort = clientRemoteAddress.replaceAll(".*:(.*)", "$1");

                ReadHoldingRegistersRequest request = service.getRequest();

                ByteBuf registers = PooledByteBufAllocator.DEFAULT.buffer(request.getQuantity());

                for (int i = 0; i < request.getQuantity(); i++) {
                    registers.writeShort(i);
                }

                service.sendResponse(new ReadHoldingRegistersResponse(registers));

                ReferenceCountUtil.release(request);
            }
        });

        slave.bind("localhost", 50200).get();
    }

    public void stop() {
        slave.shutdown();
    }

}
