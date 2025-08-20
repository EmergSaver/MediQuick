import cv2
import numpy as np

# YOLO 파일 경로
yolo_cfg = r"C:/Users/computer/darknet/yolov4-tiny.cfg"
yolo_weights = r"C:/Users/computer/darknet/yolov4-tiny.weights"
yolo_names = r"C:/Users/computer/darknet/coco.names"

# YOLO 초기화
net = cv2.dnn.readNet(yolo_weights, yolo_cfg)
with open(yolo_names, "r") as f:
    classes = [line.strip() for line in f.readlines()]

layer_names = net.getLayerNames()
unconnected = net.getUnconnectedOutLayers()
if len(unconnected.shape) == 2:
    output_layers = [layer_names[i[0] - 1] for i in unconnected]
else:
    output_layers = [layer_names[i - 1] for i in unconnected]

# 스트림 열기
stream_url = "http://192.168.0.50:8081/"
cap = cv2.VideoCapture(stream_url)

if not cap.isOpened():
    print("Stream load fail")
    exit()

while True:
    ret, frame = cap.read()
    if not ret:
        print("frame read fail")
        break

    height, width = frame.shape[:2]

    # YOLO 입력 blob 생성
    blob = cv2.dnn.blobFromImage(frame, 1/255.0, (416,416), swapRB=True, crop=False)
    net.setInput(blob)
    outs = net.forward(output_layers)

    # 탐지 결과 파싱
    class_ids = []
    confidences = []
    boxes = []

    for out in outs:
        for detection in out:
            scores = detection[5:]
            class_id = np.argmax(scores)
            confidence = scores[class_id]

            if confidence > 0.4:  # 신뢰도 threshold
                center_x = int(detection[0] * width)
                center_y = int(detection[1] * height)
                w = int(detection[2] * width)
                h = int(detection[3] * height)
                x = int(center_x - w/2)
                y = int(center_y - h/2)
                boxes.append([x, y, w, h])
                confidences.append(float(confidence))
                class_ids.append(class_id)

    # Non-max suppression
    indices = cv2.dnn.NMSBoxes(boxes, confidences, 0.5, 0.4)

    person_count = 0
    if len(indices) > 0:
        for i in indices.flatten():
            if classes[class_ids[i]] == "person":
                person_count += 1
                x, y, w, h = boxes[i]
                cv2.rectangle(frame, (x,y), (x+w, y+h), (0,255,0), 2)
                cv2.putText(frame, "Person", (x, y-10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0,255,0), 2)

    # 화면에 표시
    cv2.putText(frame, f"People Count: {person_count}", (10,30), cv2.FONT_HERSHEY_SIMPLEX, 1, (0,0,255), 2)
    cv2.imshow("YOLO People Detection", frame)

    # q 누르면 종료
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
