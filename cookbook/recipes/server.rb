include_recipe 'baragon::common'

baragon_server_jar = "BaragonService-#{node[:baragon][:version]}.jar"

remote_file "/usr/share/java/#{baragon_server_jar}" do
  action   :create
  backup   5
  owner    'root'
  group    'root'
  mode     0644
  source   "file://#{Chef::Config[:file_cache_path]}/Baragon/BaragonService" \
           "/target/#{baragon_server_jar}"
end

template '/etc/baragon/service.yml' do
  source    'service.yml.erb'
  owner     'root'
  group     'root'
  mode      0644
  notifies  :restart, 'service[baragon-server]'
end

template '/etc/init/baragon-server.conf' do
  source    'baragon-server.init.erb'
  owner     'root'
  group     'root'
  mode      0644
  notifies  :restart, 'service[baragon-server]'
  variables baragon_jar: baragon_server_jar,
            config_yaml: '/etc/baragon/service.yml'
end

service 'baragon-server' do
  provider Chef::Provider::Service::Upstart
  supports status: true,
           restart: true
  action   [:enable, :start]
end
