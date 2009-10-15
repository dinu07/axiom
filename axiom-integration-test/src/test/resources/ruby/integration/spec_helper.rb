# Copyright (c) 2009, Tim Watson
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without modification,
# are permitted provided that the following conditions are met:
#
#     * Redistributions of source code must retain the above copyright notice,
#       this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright notice,
#       this list of conditions and the following disclaimer in the documentation
#       and/or other materials provided with the distribution.
#     * Neither the name of the author nor the names of its contributors
#       may be used to endorse or promote products derived from this software
#       without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
# GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
# HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
# OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

require 'axiom'
require 'webrick'

require "uri"
uri = URI.parse "http://0.0.0.0:10001/test/inbound"

module HTTPTestListener

  def start_http! uri, &block
    fail "already listening" if running?
    @server = WEBrick::HTTPServer.new :Port => uri.port
    @server.mount_proc uri.path, &block
    @thread = Thread.new { @server.start }
  end

  def stop_http!
    return if running?
    @server.shutdown
    @thread.kill
    @thread = nil
  end

  def running?
    ((!@thread.nil?) && @thread.alive?)
  end

end

module HTTPSpecSupport
  include HTTPTestListener

  def http_interaction uri, post_data, headers={'Content-Type' => 'text/xml'}
    logger.debug("host=#{uri.host}")
    logger.debug("port=#{uri.port}")
    logger.debug("path=#{uri.path}")
    Net::HTTP.start(uri.host, uri.port) do |http|
      response = http.post(uri.path, post_data, headers)
      logger.debug("response = " + response.to_s)
      yield response if block_given?
    end
  end

end
