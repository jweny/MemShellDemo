from flask import Flask,request
from flask import render_template_string
app = Flask(__name__)

@app.route('/')
def hello_world():
    return 'Hello World'


@app.route('/test',methods=['GET', 'POST'])
def test():
    template = '''
        <div class="center-content error">
            <h1>Oops! That page doesn't exist.</h1>
            <h3>%s</h3>
        </div> 
    ''' %(request.values.get('param'))

    return render_template_string(template)


if __name__ == '__main__':
    app.run(port=8000)   