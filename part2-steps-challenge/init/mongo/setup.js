db.createCollection("user");

db.user.createIndex({username: 1}, {unique: true});
