import argparse
import json
import random
import threading
import time

import paho.mqtt.client as mqtt


def build_telemetry(schema_version, last_action):
    return {
        "schemaVersion": schema_version,
        "ts": int(time.time()),
        "firmwareVersion": "0.1.0",
        "rssi": random.randint(-70, -40),
        "battery": round(random.uniform(0.4, 1.0), 2),
        "lastAction": last_action or "idle",
    }


class DeviceSimulator:
    def __init__(self, args):
        self.args = args
        self.device_id = args.device_id
        self.secret = args.secret
        self.last_action = "idle"
        self.client = mqtt.Client(client_id=self.device_id, clean_session=True)
        self.client.username_pw_set(self.device_id, self.secret)
        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message
        self.client.on_disconnect = self.on_disconnect
        self.client.reconnect_delay_set(min_delay=1, max_delay=10)
        self.running = True

    def on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            print(f"[info] connected: deviceId={self.device_id}")
            client.subscribe(f"pet/{self.device_id}/cmd", qos=1)
        else:
            print(f"[warn] connect failed: rc={rc}")

    def on_disconnect(self, client, userdata, rc):
        if not self.running:
            return
        print(f"[warn] disconnected: rc={rc}")

    def on_message(self, client, userdata, msg):
        try:
            payload = msg.payload.decode("utf-8")
            data = json.loads(payload)
            req_id = data.get("reqId")
            cmd_type = data.get("type")
            self.last_action = cmd_type or self.last_action
            ack = {
                "schemaVersion": data.get("schemaVersion", 1),
                "reqId": req_id,
                "ok": True,
                "code": "DONE",
                "message": "ok",
                "ts": int(time.time()),
            }
        except Exception:
            ack = {
                "schemaVersion": 1,
                "reqId": None,
                "ok": False,
                "code": "BAD_PAYLOAD",
                "message": "invalid payload",
                "ts": int(time.time()),
            }
        if self.args.ack_delay_ms > 0:
            time.sleep(self.args.ack_delay_ms / 1000.0)
        self.client.publish(f"pet/{self.device_id}/cmd/ack", json.dumps(ack), qos=1)

    def telemetry_loop(self):
        while self.running:
            telemetry = build_telemetry(self.args.schema_version, self.last_action)
            self.client.publish(f"pet/{self.device_id}/telemetry", json.dumps(telemetry), qos=0)
            time.sleep(self.args.telemetry_interval)

    def reconnect_loop(self):
        if self.args.reconnect_interval <= 0:
            return
        while self.running:
            time.sleep(self.args.reconnect_interval)
            if not self.running:
                return
            print("[info] simulate disconnect")
            self.client.disconnect()
            time.sleep(self.args.reconnect_down_seconds)
            print("[info] reconnecting")
            try:
                self.client.reconnect()
            except Exception as ex:
                print(f"[warn] reconnect failed: {ex}")

    def start(self):
        self.client.connect(self.args.mqtt_host, self.args.mqtt_port, keepalive=60)
        self.client.loop_start()

        telemetry_thread = threading.Thread(target=self.telemetry_loop, daemon=True)
        telemetry_thread.start()

        reconnect_thread = threading.Thread(target=self.reconnect_loop, daemon=True)
        reconnect_thread.start()

        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            self.running = False
            self.client.disconnect()
            self.client.loop_stop()


def parse_args():
    parser = argparse.ArgumentParser(description="Deskpet MQTT 设备模拟器")
    parser.add_argument("--device-id", required=True, help="设备 ID")
    parser.add_argument("--secret", required=True, help="设备密钥")
    parser.add_argument("--mqtt-host", default="localhost", help="MQTT 地址")
    parser.add_argument("--mqtt-port", type=int, default=1883, help="MQTT 端口")
    parser.add_argument("--telemetry-interval", type=int, default=5, help="遥测上报间隔(秒)")
    parser.add_argument("--ack-delay-ms", type=int, default=200, help="回执延迟(毫秒)")
    parser.add_argument("--schema-version", type=int, default=1, help="协议版本")
    parser.add_argument("--reconnect-interval", type=int, default=0, help="断线重连周期(秒)")
    parser.add_argument("--reconnect-down-seconds", type=int, default=3, help="断开后重连等待(秒)")
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    DeviceSimulator(args).start()
