const express = require('express')
const path = require('path')
const net = require('net')
const bodyParser = require('body-parser')
const app = express()

app.use(bodyParser.json())
app.use(bodyParser.urlencoded({
  extended: true
}))
app.use(express.static(__dirname+'public'))

app.get('/', (req, res) => res.sendFile(path.join(__dirname+'/public/form.html')))

app.post('/sendEmail', (req, res) => {
  console.log(req.body)
  sendEmail(req.body)
  res.end()
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
          client.write('HELO midominio.com\n', 'utf-8', () => console.log('client wrote HELO midominio.com'))
          break
        case "250":
          okResponse += 1
          switch(okResponse){
            case 1:
              client.write('MAIL FROM: <' + info.email + '>\n', 'utf-8', () => console.log('client wrote MAIL FROM: <' + info.email + '>'))
              break
            case 2:
              client.write('RCPT TO: <' + info.email + '>\n', 'utf-8', () => console.log('client wrote RCPT TO: <' + info.email + '>'))
              break
            case 3:
              client.write('DATA\n', 'utf-8', () => console.log('DATA'))
              break
          }
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

app.listen(8080, () => console.log('Client listening on port 8080'))
