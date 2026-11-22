const jwt=require('jsonwebtoken');
function auth(req,res,next){try{const token=(req.headers.authorization||'').replace(/^Bearer /,'');if(!token)throw Error();req.user=jwt.verify(token,process.env.JWT_SECRET);next()}catch{res.status(401).json({error:'Please sign in again'})}}
function admin(req,res,next){if(req.user.role!=='admin')return res.status(403).json({error:'Admin access required'});next()}
module.exports={auth,admin};
