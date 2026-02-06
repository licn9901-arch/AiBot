import argparse
import json
import time
from typing import List

import requests


def parse_args():
    parser = argparse.ArgumentParser(description="Deskpet 指令压力冒烟脚本")
    parser.add_argument("--base-url", default="http://localhost:8080", help="pet-core 地址")
    parser.add_argument("--device-id", required=True, help="设备 ID")
    parser.add_argument("--count", type=int, default=100, help="指令次数")
    parser.add_argument("--interval-ms", type=int, default=50, help="指令间隔(毫秒)")
    parser.add_argument("--poll-interval-ms", type=int, default=200, help="查询间隔(毫秒)")
    parser.add_argument("--timeout-sec", type=int, default=15, help="单条指令超时(秒)")
    parser.add_argument("--type", default="move", help="指令类型")
    parser.add_argument("--payload", default='{"direction":"forward","speed":0.5,"durationMs":300}',
                        help="指令 payload JSON 字符串")
    return parser.parse_args()


def send_command(base_url: str, device_id: str, cmd_type: str, payload: dict) -> str:
    url = f"{base_url}/api/devices/{device_id}/commands"
    resp = requests.post(url, json={"type": cmd_type, "payload": payload}, timeout=5)
    resp.raise_for_status()
    return resp.json().get("reqId")


def wait_command(base_url: str, device_id: str, req_id: str, poll_interval_ms: int, timeout_sec: int) -> str:
    url = f"{base_url}/api/devices/{device_id}/commands/{req_id}"
    deadline = time.time() + timeout_sec
    while time.time() < deadline:
        resp = requests.get(url, timeout=5)
        if resp.status_code == 200:
            status = resp.json().get("status")
            if status in ("ACKED", "FAILED", "TIMEOUT"):
                return status
        time.sleep(poll_interval_ms / 1000.0)
    return "TIMEOUT"


def main():
    args = parse_args()
    payload = json.loads(args.payload)
    req_ids: List[str] = []
    for i in range(args.count):
        req_id = send_command(args.base_url, args.device_id, args.type, payload)
        req_ids.append(req_id)
        time.sleep(args.interval_ms / 1000.0)

    results = {"ACKED": 0, "FAILED": 0, "TIMEOUT": 0}
    for req_id in req_ids:
        status = wait_command(args.base_url, args.device_id, req_id,
                              args.poll_interval_ms, args.timeout_sec)
        results[status] = results.get(status, 0) + 1
    print("[result] " + json.dumps(results, ensure_ascii=False))


if __name__ == "__main__":
    main()
