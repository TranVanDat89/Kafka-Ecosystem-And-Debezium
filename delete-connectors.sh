#!/bin/bash
set -e
KAFKA_CONNECT_URL="http://localhost:8083"

# Hàm xoá connector
delete_connector() {
  local NAME=$1
  echo "🔎 Kiểm tra connector [$NAME]..."
  # Kiểm tra connector có tồn tại hay không
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" ${KAFKA_CONNECT_URL}/connectors/${NAME})
  if [ "$HTTP_CODE" -eq 404 ]; then
    echo "⚠️ Connector [$NAME] không tồn tại. Bỏ qua."
  elif [ "$HTTP_CODE" -eq 200 ]; then
    echo "🗑️ Đang xoá connector [$NAME]..."
    DELETE_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE ${KAFKA_CONNECT_URL}/connectors/${NAME})
    if [ "$DELETE_CODE" -eq 204 ]; then
      echo "✅ Connector [$NAME] đã xoá thành công."
    else
      echo "❌ Lỗi khi xoá connector [$NAME] (HTTP $DELETE_CODE)"
    fi
  else
    echo "❌ Lỗi khi kiểm tra trạng thái connector [$NAME] (HTTP $HTTP_CODE)"
  fi
}

# Nếu có tham số truyền vào thì dùng luôn
if [ "$#" -gt 0 ]; then
  CONNECTORS=("$@")
else
  # Nếu không có tham số thì cho phép nhập tay
  echo "👉 Nhập danh sách connector cần xoá (cách nhau bằng dấu cách):"
  read -a CONNECTORS
fi

# Kiểm tra có nhập gì không
if [ "${#CONNECTORS[@]}" -eq 0 ]; then
  echo "⚠️ Không có connector nào được nhập. Thoát."
  exit 1
fi

# Thực hiện xoá từng connector
for connector in "${CONNECTORS[@]}"; do
  delete_connector "$connector"
done

echo "🎯 Hoàn tất quá trình xoá connectors."
