from django.conf.urls import patterns, url
from django.views.generic import TemplateView

urlpatterns = patterns('viz',
    url(r'^data/(?P<filename>\w{0,50})$', 'views.data'),
    url(r'^network/(?P<filename>\w{0,50})$', 'views.network'),
                       #TemplateView.as_view(template_name='viz/network.html')),
    #url(r'^network', TemplateView.as_view(template_name='viz/network.html')),
    url(r'^/?$', 'views.list'),
)
