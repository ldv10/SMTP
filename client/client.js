const express = require('express')
const path = require('path')
const net = require('net')
const bodyParser = require('body-parser')
const app = express()

const sqlite3 = require('sqlite3').verbose();
let db = new sqlite3.Database('inbox.db');

app.use(bodyParser.json())
app.use(bodyParser.urlencoded({ extended: true }))
app.use(express.static(__dirname+'public'))

app.get('/', (req, res) => res.sendFile(path.join(__dirname+'/inbox.html')))
app.get('/form.html', (req, res) => res.sendFile(path.join(__dirname+'/form.html')))



app.post('/sendEmail', (req, res) => {
  console.log(req.body)
  sendEmail(req.body)
  res.end()
})

app.get('/retrieveMyEmails', (req,res) => {
	let resultado = [];
	let sql = `SELECT * FROM Recibidos`;

  db.serialize(function(){
  	db.each(sql, function(err,row){
  		resultado.push({ id: row.ID, sender: row.Sender, asunto: row.Asunto, contenido: row.Contenido})
  	}, () => {
  		console.log(resultado)
  		res.send(JSON.stringify(resultado))
  	})
  })
})



async function sendEmail(info){
  try {
    let client = new net.Socket()
    let okResponse = 0

    client.connect(info.port, info.host, () => {
      console.log('client connected to: ' + info.host + ':' + info.port);
    })

    client.on('data', data =>  {
      let serverResponse = String(data).substring(0,3)
      console.log('server response: ' + serverResponse)

      switch(serverResponse){
        case "200":
          client.write('HELO client@dom\n', 'utf-8', () => console.log('client wrote HELO client@dom'))
          break
        case "250":
          okResponse += 1
          switch(okResponse){
            case 1:
              client.write('MAIL FROM: <' + info.from + '>\n', 'utf-8', () => console.log('client wrote MAIL FROM: <' + info.from + '>'))
              break
            case 2:
              client.write('RCPT TO: <' + info.to + '>\n', 'utf-8', () => console.log('client wrote RCPT TO: <' + info.to + '>'))
              break
            case 3:
              client.write('DATA\n', 'utf-8', () => console.log('client wrote DATA'))
              break
            case 4:
              client.write('QUIT\n', 'utf-8', () => console.log('client wrote QUIT'))
          }
          break
        case "354":
          client.write('Subject: ' + info.subject + '\n', 'utf-8', () => console.log('client wrote Subject: ' + info.subject))
          client.write('From: ' + info.from + '\n', 'utf-8', () => console.log('client wrote From: ' + info.from))
          client.write('To: ' + info.to + '\n', 'utf-8', () => console.log('client wrote To: ' + info.to))
          client.write('\n', 'utf-8', () => console.log('client wrote new line'))
          client.write(info.message + '\n', 'utf-8', () => console.log('client wrote ' + info.message))
          client.write('.\n', 'utf-8', () => console.log('client wrote .'))
          break
        case "221":
          client.end()
          break
        default:
          console.log('not defined ' + data)
      }
    })

    client.on('close', function() {
      console.log('client closed')
    })
   
    client.on('error', function(err) {
      console.error(err)
    })

  } catch(e) {
    throw e
  }
}



app.listen(8082, () => console.log('Client listening on port 8082'))
