import warnings
warnings.filterwarnings("ignore", category=FutureWarning)

import cv2
import torch
import time
import mysql.connector
from datetime import datetime

# YOLOv5 모델 로드
model = torch.hub.load('ultralytics/yolov5', 'yolov5s')
model.eval()

# MySQL 연결
db = mysql.connector.connect(
    host="localhost",
    user="root",
    password="1234",
    database="sys"
)
cursor = db.cursor()
print("Connect success")

# 비디오 스트림 열기
cap = cv2.VideoCapture("http://192.168.0.50:8081/")

if not cap.isOpened():
    print("Stream load fail")
    exit()

last_print_time = time.time()

while True:
    ret, frame = cap.read()
    if not ret or frame is None:
        print("Frame read fail, retrying...")
        continue

    # YOLO 탐지
    with torch.no_grad():
        results = model(frame)

    person_count = 0
    current_confidences = []

    for *xyxy, conf, cls in results.xyxy[0].tolist():
        if int(cls) == 0 and conf > 0.5:
            person_count += 1
            current_confidences.append(conf)
            x1, y1, x2, y2 = map(int, xyxy)
            cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
            cv2.putText(frame, f"Person: {conf:.2f}", (x1, y1 - 10),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)

    # 영상에 사람 수 표시
    cv2.putText(frame, f"People Count: {person_count}", (10, 30),
                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
    cv2.imshow("YOLOv5 People Detection", frame)

    # 3초마다 콘솔 출력 + DB 저장
    current_time = time.time()
    if current_time - last_print_time >= 5:
        now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

        avg_conf = (sum(current_confidences) / person_count) if person_count > 0 else 0.0
        print(f"Person count: {person_count}, Average accuracy: {avg_conf:.2f}, Time: {now}")

        # DB 저장
        try:
            sql = "INSERT INTO analysis (people_count, accuracy, datetime) VALUES (%s, %s, %s)"
            val = (person_count, avg_conf, now)
            cursor.execute(sql, val)
            db.commit()
        except Exception as e:
            print("DB 저장 오류:", e)

        last_print_time = current_time

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# 자원 정리
cap.release()
cv2.destroyAllWindows()
cursor.close()
db.close()