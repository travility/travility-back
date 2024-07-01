package travility_back.travility.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import travility_back.travility.dto.oauth.CustomOAuthUser;
import travility_back.travility.dto.oauth.NaverOAuth2LoginDto;
import travility_back.travility.dto.oauth.response.OAuth2Response;
import travility_back.travility.entity.Member;
import travility_back.travility.entity.enums.Role;
import travility_back.travility.repository.MemberRepository;
import travility_back.travility.dto.oauth.response.GoogleResponse;
import travility_back.travility.dto.oauth.response.NaverResponse;

import java.security.AuthProvider;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Response oAuth2Response = null;
        Member member = new Member();

        String accessToken = userRequest.getAccessToken().getTokenValue();

        if (registrationId.equals("naver")) {
            oAuth2Response = new NaverResponse(oAuth2User.getAttributes()); // 초기화
            member.setSocialType("naver");
        }
        else if (registrationId.equals("google")) {
            oAuth2Response = new GoogleResponse(oAuth2User.getAttributes()); // 초기화
            member.setSocialType("google");
        }
        else {
            return null;
        }

        // 리소스 서버에서 발급받은 정보로 아이디값 만들기
        String username = oAuth2Response.getProvider() + "_" + oAuth2Response.getProviderId();

        // 해당 유저가 이미 로그인 했는지
        Optional<Member> isAlreadyLogin = memberRepository.findByUsername(username);

        // 한번도 로그인하지 않아서 null인경우
        if (isAlreadyLogin.isEmpty()) {

            member.setUsername(username);
            member.setName(oAuth2Response.getName());
            member.setEmail(oAuth2Response.getEmail());
            member.setRole(Role.ROLE_USER);
            member.setCreatedDate(LocalDateTime.now());
            member.setAccessToken(accessToken);

            memberRepository.save(member);

            // dto에 저장
            NaverOAuth2LoginDto naverDto = new NaverOAuth2LoginDto();
            naverDto.setUsername(username);
            naverDto.setName(oAuth2Response.getName());
            naverDto.setRole(Role.ROLE_USER);

            return new CustomOAuthUser(naverDto);

        }
        // 한번이라도 로그인을 진행해서 데이터가 존재하는경우
        else {
            // 데이터를 업데이트해줘야함
            Member existData = isAlreadyLogin.get();
            existData.setEmail(oAuth2Response.getEmail());
            existData.setName(oAuth2Response.getName());
            existData.setAccessToken(accessToken);

            memberRepository.save(existData);

            // dto에 저장
            NaverOAuth2LoginDto naverDto = new NaverOAuth2LoginDto();
            naverDto.setUsername(username);
            // name은 바뀐걸로 갖고와야해서 oAuth2Response에서 갖고옴
            naverDto.setName(oAuth2Response.getName());
            naverDto.setRole(existData.getRole());

            return new CustomOAuthUser(naverDto);

        }
    }
}
