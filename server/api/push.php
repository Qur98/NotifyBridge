<?php
header('Content-Type: application/json; charset=utf-8'); header('Cache-Control: no-store');
try{
 $c=require __DIR__.'/../config/config.php'; $key=$_SERVER['HTTP_X_API_KEY']??'';
 if(!is_string($key)||$key===''){http_response_code(401);echo json_encode(['ok'=>false,'error'=>'unauthorized']);exit;}
 require __DIR__.'/../config/db.php';
 $auth=false;$apiId=0;
 try{$ak=$pdo->prepare('SELECT id FROM api_keys WHERE api_key=? AND is_active=1 LIMIT 1');$ak->execute([$key]);$ar=$ak->fetch();if($ar){$auth=true;$apiId=(int)$ar['id'];}}catch(Throwable $ignore){}
 if(!$auth && !empty($c['api_key']) && hash_equals((string)$c['api_key'],$key)){$auth=true;}
 if(!$auth){http_response_code(401);echo json_encode(['ok'=>false,'error'=>'unauthorized']);exit;}
 if(($_SERVER['REQUEST_METHOD']??'GET')!=='POST'){http_response_code(405);header('Allow: POST');echo json_encode(['ok'=>false,'error'=>'method_not_allowed']);exit;}
 $d=json_decode(file_get_contents('php://input'),true); if(!is_array($d)){http_response_code(400);echo json_encode(['ok'=>false,'error'=>'invalid_json']);exit;}
 foreach(['device_id','app_package','notification_key'] as $f){if(empty($d[$f])){http_response_code(422);echo json_encode(['ok'=>false,'error'=>'missing_'.$f]);exit;}}
 if($apiId){$u=$pdo->prepare('UPDATE api_keys SET last_used_at=NOW(),use_count=use_count+1 WHERE id=?');$u->execute([$apiId]);}
 $device=mb_substr((string)$d['device_id'],0,100); $pkg=mb_substr((string)$d['app_package'],0,255); $name=mb_substr((string)($d['app_name']??''),0,255); $ip=mb_substr((string)($_SERVER['REMOTE_ADDR']??''),0,64);
 $pdo->beginTransaction();
 $s=$pdo->prepare('INSERT IGNORE INTO notifications(device_id,app_package,app_name,title,message,notification_key,sent_at) VALUES(?,?,?,?,?,?,?)');
 $s->execute([$device,$pkg,$name,mb_substr((string)($d['title']??''),0,500),(string)($d['message']??''),mb_substr((string)$d['notification_key'],0,255),mb_substr((string)($d['sent_at']??''),0,64)]); $inserted=$s->rowCount()>0;
 $q=$pdo->prepare("INSERT INTO devices(device_id,last_ip,notification_count) VALUES(?,?,?) ON DUPLICATE KEY UPDATE last_seen=NOW(),last_ip=VALUES(last_ip),notification_count=notification_count+VALUES(notification_count),status='online'"); $q->execute([$device,$ip,$inserted?1:0]);
 $q=$pdo->prepare('INSERT INTO apps(app_package,app_name,notification_count) VALUES(?,?,?) ON DUPLICATE KEY UPDATE app_name=IF(VALUES(app_name)<>\'\',VALUES(app_name),app_name),last_seen=NOW(),notification_count=notification_count+VALUES(notification_count),is_active=1'); $q->execute([$pkg,$name,$inserted?1:0]);
 if($inserted){$q=$pdo->prepare('INSERT INTO activity_logs(event_type,device_id,app_package,details,ip_address) VALUES(?,?,?,?,?)');$q->execute(['notification_received',$device,$pkg,mb_substr((string)($d['title']??'إشعار جديد'),0,1000),$ip]);}
 $pdo->commit(); echo json_encode(['ok'=>true,'inserted'=>$inserted],JSON_UNESCAPED_UNICODE);
}catch(Throwable $e){if(isset($pdo)&&$pdo->inTransaction())$pdo->rollBack();http_response_code(500);echo json_encode(['ok'=>false,'error'=>'server_error']);}
