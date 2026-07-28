GET /api/schedule?group=&date=
GET /api/schedule/today?group=
GET /api/schedule/tomorrow?group= // TODO:FIX
GET /api/schedule/yesterday?group= 
GET /api/schedule/week?group=        (обязателен) // TODO
GET /api/schedule/current-weektype

GET /api/groups
GET /api/teachers
GET /api/subjects
GET /api/rooms

GET /api/search?teacher=&group=&room=&subject=&date=&pair= // DELETED, пока не понадобится 

GET /api/admin/heartbeats
GET /api/admin/heartbeats/latest
GET /api/admin/heartbeats/{id}
GET /api/admin/snapshots
GET /api/admin/snapshots/{id}