<?php
session_start();
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
if(empty($_SESSION['admin'])){http_response_code(401);echo json_encode(['error'=>'unauthorized']);exit;}
try{
 require __DIR__.'/../config/db.php';
 $after=max(0,(int)($_GET['after']??0));
 $s=$pdo->prepare('SELECT * FROM notifications WHERE id>? ORDER BY id ASC LIMIT 100');
 $s->execute([$after]);
 echo json_encode($s->fetchAll(),JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES);
}catch(Throwable $e){http_response_code(500);echo json_encode(['error'=>'server_error']);}
