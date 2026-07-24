GET /api/schedule?group=&date=
GET /api/schedule/today?group=
GET /api/schedule/tomorrow?group= // TODO:FIX
GET /api/schedule/yesterday?group= // TODO:FIX
GET /api/schedule/week?group=        (обязателен)
GET /api/schedule/current-weektype

GET /api/groups
GET /api/teachers
GET /api/subjects
GET /api/rooms

GET /api/search?teacher=&group=&room=&subject=&date=&pair= // TODO:FIX

GET /api/admin/heartbeats
GET /api/admin/heartbeats/latest
GET /api/admin/heartbeats/{id}
GET /api/admin/snapshots
GET /api/admin/snapshots/{id}
GET /api/admin/validation // TODO:FIX
GET /api/admin/validation/{snapshotId} // TODO:FIX